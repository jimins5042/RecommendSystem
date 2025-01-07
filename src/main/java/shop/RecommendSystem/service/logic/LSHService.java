package shop.RecommendSystem.service.logic;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import shop.RecommendSystem.dto.ImageInfo;
import shop.RecommendSystem.repository.mapper.SearchMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

@Service
@RequiredArgsConstructor
@Slf4j
public class LSHService {

    private final SearchMapper searchMapper;

    private HashMap<String, HashSet<ImageInfo>> lshBuckets = new HashMap<>();
    private int windowSize = 4;

    //프로젝트 시작시 실행
    @PostConstruct
    public void initializeLSH() {
        ArrayList<ImageInfo> images = searchMapper.findSearchTarget();

        //객체 안에는 imageUuid, imageHashCode가 존재
        for (ImageInfo image : images) {
            addLshBucket(image);
        }
        log.info("=== LSH buckets initialized ===");
    }

    public HashMap searchLSH(String hashValue) {
        // StringBuilder 생성
        StringBuilder sb = new StringBuilder(hashValue);
        ArrayList<String> duplicateCheck = new ArrayList<>();
        HashMap<String, Double> candidates = new HashMap<>();

        for (int i = 0; i <= hashValue.length() - windowSize; i++) {
            // 슬라이딩 윈도우로 문자열 추출
            String key = sb.substring(i, i + windowSize);
            if (!duplicateCheck.contains(key)) {

                duplicateCheck.add(key);

                if (lshBuckets.containsKey(key)) {
                    for (ImageInfo image : lshBuckets.get(key)) {
                        Double hammingDistance = calHammingDistance(hashValue, image.getImageHashCode());
                        candidates.put(image.getImageUuid(), hammingDistance);
                        log.info("candidates : {}", candidates.get(image.getImageUuid()));
                    }
                }

            }
        }

        return candidates;
    }

    public double calHammingDistance(String a, String b) {

        long hammingDistance = 0;

        for (int i = 0; i < a.length(); i++) {
            if (a.charAt(i) != b.charAt(i)) {
                hammingDistance++;
            }
        }

        return hammingDistance;
    }


    public void addLshBucket(ImageInfo image) {

        // StringBuilder 생성
        StringBuilder sb = new StringBuilder(image.getImageHashCode());

        for (int i = 0; i <= image.getImageHashCode().length() - windowSize; i++) {
            // 슬라이딩 윈도우로 문자열 추출
            String key = sb.substring(i, i + windowSize);

            // 키가 이미 존재하면 Set에 itemUuid 추가, 존재하지 않으면 새로운 Set 생성
            if (!lshBuckets.containsKey(key)) {
                lshBuckets.put(key, new HashSet<>());
            }
            lshBuckets.get(key).add(image); // 값 정상 입력 확인

        }
    }

    public void removeLshBucket(String hashValue, String itemUuid) {

        // StringBuilder 생성
        StringBuilder sb = new StringBuilder(hashValue);

        for (int i = 0; i <= hashValue.length() - windowSize; i++) {
            // 슬라이딩 윈도우로 문자열 추출
            String key = sb.substring(i, i + windowSize);

            // 버킷에 itemUuid가 존재하면 Set에서 itemUuid 삭제, Set 안에 값이 없으면 버킷 삭제
            lshBuckets.get(key).remove(itemUuid);   //??
            if (lshBuckets.get(key).size() == 0) {
                lshBuckets.remove(key);
            }
        }
    }

}
