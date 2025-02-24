package shop.RecommendSystem.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import shop.RecommendSystem.dto.SearchResult;
import shop.RecommendSystem.recommend.ImageFeature.PHash;
import shop.RecommendSystem.recommend.ImageFeature.VGG16;
import shop.RecommendSystem.recommend.ItemFiltering.BitwiseANDFiltering;
import shop.RecommendSystem.recommend.ItemFiltering.MinHashFiltering;
import shop.RecommendSystem.recommend.ItemFiltering.PrefixFiltering;
import shop.RecommendSystem.repository.mapper.SearchMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final PrefixFiltering prefix;
    private final BitwiseANDFiltering filter;
    private final VGG16 vgg16;
    private final SearchMapper searchMapper;
    private final MinHashFiltering minHashFiltering;

    /**
     * 유사 이미지 검색 테스트 페이지에서
     * 여러 탐색 방법을 비교하는 함수
     *
     * @param file : 질의 이미지
     * @param resultSize :  반환핳 검색 결과 수
     * @param searchWay : 검색 방법
     * @return
     * @throws Exception
     */

    public List<SearchResult> searchSimilarItems(MultipartFile file, int resultSize, String searchWay) throws Exception {
        HashMap<String, Double> map = new HashMap<>();

        if (searchWay.equals("pHash")) {

            String hashValue = new PHash().getPHash(file);
            map = prefix.searchSimilarItem(hashValue);

        } else if (searchWay.equals("VGG16")) {
            Map<String, Object> req = vgg16.sendImageToFastAPI(file);

            String order = (String) req.get("order");
            byte[] feature = (byte[]) req.get("features");

            map = filter.searchSimilarItem(order, feature);

        } else if (searchWay.equals("LSH")) {
            Map<String, Object> req = vgg16.sendImageToFastAPI(file);

            String order = (String) req.get("order");
            byte[] feature = (byte[]) req.get("features");

            map = minHashFiltering.searchSimilarItem(order, feature);

        }
        return searchSimilarItems(map, resultSize);
    }

    /**
     * 상품의 uuid와 유사도가 저장된 HashMap을 바탕으로 상품 정보를 DB에서 가져오고, 유사도 순으로 정렬하여 탐색 결과를 반환
     *
     * @param map        : 상품의 uuid와 유사도가 저장된 HashMap
     * @param resultSize : 반환할 탐색 결과 갯수
     * @return : 유사도가 큰 순으로 정렬된 상품 정보 데이터
     */
    public List<SearchResult> searchSimilarItems(HashMap<String, Double> map, int resultSize) {

        // 유사도를 기준으로 내림차 정렬
        ArrayList<String> keySet = new ArrayList<>(map.keySet());
        keySet.sort((o1, o2) -> map.get(o2).compareTo(map.get(o1)));
        List<SearchResult> results = new ArrayList<>();

        if (keySet.size() < 1) {
            return results;
        }

        // LSH에서 탐색한 상품 후보군 중, 유사도가 높은 상위 {resultSize}개의 상품 정보를 가져옴
        results = searchMapper
                .findItemCandidates(
                        //만약 resultSize가 상품 후보군의 전체 수보다 크다면 -> 전체 상품 정보 가져옴
                        keySet.subList(0, (resultSize > keySet.size()) ? keySet.size() : resultSize)
                        //keySet.subList(0, keySet.size())
                );

        ExecutorService executor = Executors.newFixedThreadPool(10); // 최대 10개의 스레드를 사용
        List<CompletableFuture<Void>> futures = results.stream()
                .map(item -> CompletableFuture.runAsync(() -> {
                    try {
                        item.setHammingDistance(map.get(item.getImageUuid()));
                        log.info("hamming = {}", item.getHammingDistance());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, executor))  // Executor를 명시적으로 지정
                .collect(Collectors.toList());

        // 모든 작업이 완료될 때까지 기다림
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        executor.shutdown(); // 작업 완료 후 Executor 종료

        // DB에서 상품 정보를 반환 받을 때 uuid의 사전순으로 반환 받으므로, 유사도 순으로 재정렬
        results.sort((o1, o2) -> o2.getHammingDistance().compareTo(o1.getHammingDistance()));

        return results;
    }


    /**
     * pHash를 이용해 유사 이미지를 탐색하는 함수
     *
     * @param hashcode   : 16진수로 변환된 pHash 값
     * @param resultSize : 반환받을 탐색 결과 갯수
     * @return : 유사도가 큰 순으로 정렬된 상품 정보 데이터
     */

    public List<SearchResult> searchSimilarItems(String hashcode, int resultSize) {

        HashMap<String, Double> map = prefix.searchSimilarItem(hashcode);
        return searchSimilarItems(map, resultSize);
    }

    public List<SearchResult> searchSimilarItems(String order, byte[] feature, int resultSize) throws JsonProcessingException {

        HashMap<String, Double> map = filter.searchSimilarItem(order, feature);
        return searchSimilarItems(map, resultSize);
    }
}
