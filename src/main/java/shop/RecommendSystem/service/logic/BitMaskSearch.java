package shop.RecommendSystem.service.logic;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import shop.RecommendSystem.dto.ImageInfo;
import shop.RecommendSystem.repository.mapper.SearchMapper;

import java.math.BigInteger;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BitMaskSearch {

    private final SearchMapper searchMapper;

    private TreeMap<String, String> bitmaskTree = new TreeMap<>();

    @PostConstruct
    public void initializeBitMaskTree() {
        ArrayList<ImageInfo> images = searchMapper.findSearchBitMaskTarget();

        //객체 안에는 imageUuid, imageHashCode가 존재
        for (ImageInfo image : images) {
            bitmaskTree.put(image.getImageHashCode(), image.getImageUuid());
        }
        log.info("=== Bitmask TreeMap initialized ===");
    }

    public String searchBitMask(List<Double> targetBitFlagList) {
        long beforeTime = System.currentTimeMillis();
        TreeMap<String, String> copyTree = new TreeMap<>(bitmaskTree);

        // 1. 새로운 bitSet 생성 및 초기화
        byte[] bitSet = new byte[32];
        for (Double targetBitFlag : targetBitFlagList) {

            bitSet = encodeFeaturesAsHex(targetBitFlag, bitSet);
            String targetHex = binaryToHex(bitSet);

            // 2. targetHex 이상의 값들만 필터링
            SortedMap<String, String> sortTree = copyTree.tailMap(targetHex);  // targetHex보다 큰 값을 선택

            // 3. TreeMap에서 조건에 맞는 키 검색
            TreeMap<String, String> filteredTree = new TreeMap<>();
            for (Map.Entry<String, String> entry : sortTree.entrySet()) {
                String key = entry.getKey();

                // 키와 targetHex의 비트 연산 확인
                if (checkBitCondition(key, targetHex)) {
                    filteredTree.put(key, entry.getValue());
                }
            }

            // 4. 필터링된 TreeMap으로 갱신
            copyTree = filteredTree;

            // 5. 리스트 크기가 100 미만이면 루프 종료
            if (copyTree.size() < 100) {
                break;
            }
        }

        Collection<String> keys = copyTree.values();
        log.info("=== Bitmask TreeMap search ===. {} ", System.currentTimeMillis() - beforeTime);

        for (String key : keys) {
            log.info("bitmask : {}", key);
        }

        return "";
    }

    private boolean checkBitCondition(String keyHex, String targetHex) {
        // 16진수 문자열을 BigInteger로 변환
        BigInteger key = new BigInteger(keyHex, 16);
        BigInteger target = new BigInteger(targetHex, 16);

        // 비트 연산 조건 확인
        return key.and(target).equals(target);
    }

    // 비트를 16진수로 변환하기 위한 함수
    private byte[] encodeFeaturesAsHex(Double key, byte[] bitSet) {
        int index = key.intValue(); // 특징점 값을 인덱스로 사용
        if (index < 256) { // 유효한 범위 내에서만 처리
            int byteIndex = index / 8; // 바이트 배열에서의 위치
            int bitIndex = index % 8;  // 바이트 내의 비트 위치
            bitSet[byteIndex] |= (1 << (7 - bitIndex)); // 해당 비트를 1로 설정
        } else {
            // 범위 초과 시 로그 추가
            System.err.println("Warning: Key out of range (key=" + key + ")");
        }
        return bitSet;
    }

    // 바이트 배열을 16진수 문자열로 변환
    public String binaryToHex(byte[] bitSet) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bitSet) {
            hexString.append(String.format("%02X", b));
        }
        return hexString.toString();
    }

}
