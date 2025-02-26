package shop.RecommendSystem.recommend.ItemFiltering;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import shop.RecommendSystem.dto.ImageInfo;
import shop.RecommendSystem.repository.mapper.SearchMapper;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

@Component
@RequiredArgsConstructor
@Slf4j
public class PrefixFiltering {

    private final SearchMapper searchMapper;

    private HashMap<String, HashSet<ImageInfo>> buckets = new HashMap<>();
    private int windowSize = 4;

    //프로젝트 시작시 실행
    @PostConstruct
    public void initializeSearchData() {
        ArrayList<ImageInfo> images = searchMapper.findSearchPHashTarget();

        //객체 안에는 imageUuid, imageHashCode가 존재
        for (ImageInfo image : images) {
            addSearchData(image);
        }
        log.info("=== pHash buckets initialized ===");
    }


    public HashMap<String, Double> searchSimilarItem(String hashValue) {
        // StringBuilder 생성
        StringBuilder sb = new StringBuilder(hashValue);
        HashSet<String> duplicateCheck = new HashSet<>();
        HashMap<String, Double> candidates = new HashMap<>();

        for (int i = 0; i <= hashValue.length() - windowSize; i++) {
            // 슬라이딩 윈도우로 문자열 추출
            String key = sb.substring(i, i + windowSize);

            // 이미 탐색한 버킷키가 아니고, 존재하는 버킷키일 경우
            if (!duplicateCheck.contains(key) && buckets.containsKey(key)) {

                for (ImageInfo image : buckets.get(key)) {
                    Double hammingDistance = 1 - calHammingDistance(hashValue, image.getImageHashCode());
                    candidates.put(image.getImageUuid(), hammingDistance);

                    //유사도가 60% 미만인 상품은 후보군에서 제외
                    if (hammingDistance >= 0.5) {
                        candidates.put(image.getImageUuid(), hammingDistance);
                    }

                }
            }
            duplicateCheck.add(key);
        }

        return candidates;
    }

    public void addSearchData(ImageInfo image) {

        // StringBuilder 생성
        StringBuilder sb = new StringBuilder(image.getImageHashCode());

        for (int i = 0; i <= image.getImageHashCode().length() - windowSize; i++) {
            // 슬라이딩 윈도우로 문자열 추출
            String key = sb.substring(i, i + windowSize);

            // 키가 이미 존재하면 Set에 itemUuid 추가, 존재하지 않으면 새로운 Set 생성
            if (!buckets.containsKey(key)) {
                buckets.put(key, new HashSet<>());
            }
            buckets.get(key).add(image); // 값 정상 입력 확인

        }
    }

    public void removeLshBucket(String hashValue, String itemUuid) {

        // StringBuilder 생성
        StringBuilder sb = new StringBuilder(hashValue);

        for (int i = 0; i <= hashValue.length() - windowSize; i++) {
            // 슬라이딩 윈도우로 문자열 추출
            String key = sb.substring(i, i + windowSize);

            // 버킷에 itemUuid가 존재하면 Set에서 itemUuid 삭제, Set 안에 값이 없으면 버킷 삭제
            buckets.get(key).remove(itemUuid);   //??
            if (buckets.get(key).size() == 0) {
                buckets.remove(key);
            }
        }
    }

    public double calHammingDistance(String keyHex, String targetHex) {
        // 16진수 문자열을 BigInteger로 변환
        BigInteger key = new BigInteger(keyHex, 16);
        BigInteger target = new BigInteger(targetHex, 16);
        //대칭 차집합
        int symmetricDifference = key.xor(target).bitCount();

        return (double) symmetricDifference / target.bitLength();
    }

}
