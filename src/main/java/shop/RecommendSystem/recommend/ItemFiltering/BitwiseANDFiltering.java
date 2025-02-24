package shop.RecommendSystem.recommend.ItemFiltering;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import shop.RecommendSystem.dto.PreFilterDto;
import shop.RecommendSystem.repository.mapper.SearchMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class BitwiseANDFiltering {

    private final SearchMapper searchMapper;

    private ArrayList<PreFilterDto> list = new ArrayList<>();

    // 프로젝트 시작시 실행
    @PostConstruct
    public void initializeSearchData() throws JsonProcessingException {
        ArrayList<PreFilterDto> images = searchMapper.findReduceTarget();

        for (PreFilterDto image : images) {

            PreFilterDto preFilter = PreFilterDto.builder()
                    .imageUuid(image.getImageUuid())
                    .layerOrderList(addSearchData(image.getFeatureOrder()))
                    .build();

            list.add(preFilter);
        }
        log.info("=== PreFilter list initialized ===");
    }


    // 추출한 25개의 레이어의 번호를 512비트 크기의 비트 벡터로 변환 후 저장
    public byte[] addSearchData(String list) throws JsonProcessingException {
        byte[] bitArray = new byte[64];
        ObjectMapper objectMapper = new ObjectMapper();

        int[] paletteArray = objectMapper.readValue(list, int[].class);

        for (int pos : paletteArray) {
            int byteIndex = pos / 8;  // 해당 비트가 속한 바이트 위치
            int bitIndex = pos % 8;   // 바이트 내부에서 비트 위치
            bitArray[byteIndex] |= (1 << bitIndex);  // 비트 설정
        }

        return bitArray;
    }

    /**
     * VGG16을 이용한 유사 이미지 탐색 함수
     *
     * @param layerList      : JSON형식으로 저장된, Intensity 평균값의 크기순으로 정렬된 레이어 번호 들
     * @param targetBitArray : 이미지의 특징점
     * @return : 이미지의 uuid와 유사도가 저장된 HashMap
     * @throws JsonProcessingException
     */
    public HashMap searchSimilarItem(String layerList, byte[] targetBitArray) throws JsonProcessingException {

        HashMap<String, Double> similarImage = new HashMap<>();

        // 1. 추출한 레이어의 크기 순서를 이용하여, 비교할 이미지 후보군의 수를 300개 이하로 줄임
        ArrayList<PreFilterDto> preprocess = preprocessList(layerList);

        // 2. 이미지 후보군의 uuid만 추린다음 db에서 이미지의 특징점을 읽어옴
        List<String> uuidList = preprocess.stream().map(PreFilterDto::getImageUuid).collect(Collectors.toList());
        ArrayList<PreFilterDto> candidates = searchMapper.findPreFilterTarget(uuidList);

        // 3. 코사인 유사도를 이용하여 유사도 계산 후 저장
        for (PreFilterDto candidate : candidates) {
            similarImage.put(candidate.getImageUuid(), cosineSimilarity(targetBitArray, candidate.getImgFeatureValue()));
        }

        System.out.println("vggnet");

        return similarImage;
    }


    /**
     * 1-1. 유사도 게산 전, 검색 범위를 줄이기 위한 전처리 과정
     * 문자열로 저장된 질의 이미지의 레이어 번호를 int[] 배열로 변환
     *
     * @param layerList : JSON형식으로 저장된, Intensity 평균값의 크기순으로 정렬된 레이어 번호들
     * @return : 유사도를 계산할 이미지의 후보군을 300개 이하로 줄인 다음 반환
     * @throws JsonProcessingException
     */
    public ArrayList<PreFilterDto> preprocessList(String layerList) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        int[] layer = objectMapper.readValue(layerList, int[].class);
        return reduceScope(list, layer, 0);
    }

    /**
     * 1-2. 검색 범위를 줄이는 함수
     * 가장 크게 반응한(Intensity가 큰) 레이어 번호 순으로, 그 레이어를 가지는 이미지를 모두 찾는다.
     * <p>
     * 1) num 번째 레이어를 가진 상품번호를 AND 비트 연산을 통해 찾는다.
     * 2) 위의 방법으로 num+1 번째, num+2 번째 레이어도 가지고 있는 이미지를 찾는다.
     * 그러면 검색범위는 점점 줄어들게 된다.
     * 3) 이미지 리스트가 300개 이하가 되거나, 25개의 레이어번호를 모두 순회했다면 이미지 리스트를 반환
     *
     * @param list  : 줄여나가는 검색 범위
     * @param layer : 질의 이미지의 레이어 번호 목록
     * @param num   : 탐색할 레이어의 위치
     * @return : 전처리가 끝난 이미지 리스트
     */

    public ArrayList<PreFilterDto> reduceScope(ArrayList<PreFilterDto> list, int[] layer, int num) {

        ArrayList<PreFilterDto> reducedList = new ArrayList<>();    //빈 리스트 생성
        int byteIndex = layer[num] / 8;  // 해당 비트가 속한 바이트 위치
        int bitIndex = layer[num] % 8;   // 바이트 내부에서 비트 위치
        byte target = (byte) (1 << bitIndex);  // 비트 설정

        for (PreFilterDto preFilterDto : list) {
            if ((preFilterDto.getLayerOrderList()[byteIndex] & target) != 0) {
                reducedList.add(preFilterDto);
            }
        }
        // 이미지 후보군의 수가 300개 이하거나, 더이상 탐색할 레이어 번호가 없다면 리스트를 반환
        if (reducedList.size() <= 350 || num == 24) {
            return reducedList;
        } else {
            // 아니라면 탐색 범위를 더 줄이기
            return reduceScope(reducedList, layer, num + 1);
        }
    }


    // 코사인 유사도 계산 공식
    public static double cosineSimilarity(byte[] A, byte[] B) {
        int dotProduct = 0;
        int normA = 0;
        int normB = 0;

        for (int i = 0; i < A.length; i++) {
            dotProduct += Integer.bitCount(A[i] & B[i]); // A · B (AND 후 1의 개수)
            normA += Integer.bitCount(A[i]);  // A의 1 개수
            normB += Integer.bitCount(B[i]);  // B의 1 개수
        }

        if (normA == 0 || normB == 0) return 0;  // 벡터가 0인 경우 처리
        return (double) dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

}
