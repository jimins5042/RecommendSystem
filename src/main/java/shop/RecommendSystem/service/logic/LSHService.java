package shop.RecommendSystem.service.logic;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import shop.RecommendSystem.dto.ImageInfo;
import shop.RecommendSystem.repository.mapper.SearchMapper;

import java.util.*;

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
        HashSet<String> duplicateCheck = new HashSet<>();
        HashMap<String, Double> candidates = new HashMap<>();
        String hashValue16 = new java.math.BigInteger(hashValue, 16).toString(2);
        for (int i = 0; i <= hashValue.length() - windowSize; i++) {
            // 슬라이딩 윈도우로 문자열 추출
            String key = sb.substring(i, i + windowSize);

            // 이미 탐색한 버킷키가 아니고, 존재하는 버킷키일 경우
            if (!duplicateCheck.contains(key) && lshBuckets.containsKey(key)) {

                for (ImageInfo image : lshBuckets.get(key)) {
                    Double hammingDistance = calHammingDistance(hashValue16, image.getImageHashCode());

                    //유사도가 60% 미만인 상품은 후보군에서 제외
                    //if (hammingDistance <= 0.4) {
                    candidates.put(image.getImageUuid(), hammingDistance);
                    //}
                }
            }
            duplicateCheck.add(key);
        }

        return candidates;
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

    public double calHammingDistance(String a, String b) {

        String binary = new java.math.BigInteger(b, 16).toString(2);

        long hammingDistance = 0;

        for (int i = 0; i < binary.length(); i++) {
            if (a.charAt(i) != binary.charAt(i)) {
                hammingDistance++;
            }
        }
        return (double) hammingDistance / binary.length();
    }


    public double calJaccardSimilarity(Set<String> a, Set<String> b) {
        // 원본 집합을 보존하기 위해 복사본을 사용
        Set<String> intersection = new HashSet<>(a);
        Set<String> union = new HashSet<>(a);

        // 교집합 계산
        intersection.retainAll(b);

        // 합집합 계산
        union.addAll(b);

        // 자카드 유사도 계산
        if (union.size() == 0) {
            return 0.0; // 합집합이 비어 있을 경우 0 반환
        }

        return (double) intersection.size() / union.size();
    }


    public double calJaccardSimilarity(String aarr, String barr) {

        Set<String> a = new HashSet<>(Arrays.asList(aarr.split(",")));
        Set<String> b = new HashSet<>(Arrays.asList(barr.split(",")));

        Set<String> intersection = new HashSet<>(a);
        Set<String> union = new HashSet<>(a);

        // 교집합 계산
        intersection.retainAll(b);

        // 합집합 계산
        union.addAll(b);

        // 자카드 유사도 계산
        if (union.size() == 0) {
            return 0.0; // 합집합이 비어 있을 경우 0 반환
        }

        return (double) intersection.size() / union.size();
    }

}
