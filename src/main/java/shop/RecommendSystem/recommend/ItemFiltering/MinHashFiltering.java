package shop.RecommendSystem.recommend.ItemFiltering;

import info.debatty.java.lsh.MinHash;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import shop.RecommendSystem.dto.PreFilterDto;
import shop.RecommendSystem.repository.mapper.SearchMapper;

import java.util.ArrayList;
import java.util.HashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class MinHashFiltering {
    private final SearchMapper searchMapper;
    private HashMap<String, int[]> minHashMap = new HashMap<>();

    //@PostConstruct
    public void initializeSearchData() {
        try {
            ArrayList<PreFilterDto> images = searchMapper.findLshTarget();

            byte[] firstFeatureValue = images.get(0).getImgFeatureValue();

            MinHash minHash = new MinHash(500, firstFeatureValue.length * 8, 123456);

            for (PreFilterDto image : images) {
                if (image.getImgFeatureValue() == null) continue; // Null 방지
                int[] signature = minHash.signature(byteArrayToBooleanArray(image.getImgFeatureValue()));
                minHashMap.put(image.getImageUuid(), signature);
            }

            log.info("=== MinHash list initialized ===");
        } catch (Exception e) {
            log.error("MinHash 초기화 중 오류 발생", e);
            throw new RuntimeException("MinHash 초기화 중 오류 발생", e);
        }
    }

    public boolean[] byteArrayToBooleanArray(byte[] byteArray) {
        boolean[] boolArray = new boolean[byteArray.length * 8];

        for (int i = 0; i < byteArray.length; i++) {
            for (int j = 0; j < 8; j++) {
                boolArray[i * 8 + j] = ((byteArray[i] >> (7 - j)) & 1) == 1;
            }
        }
        return boolArray;
    }

    public HashMap<String, Double> searchSimilarItem(String layerList, byte[] targetBitArray) {
        HashMap<String, Double> results = new HashMap<>();

        if (targetBitArray == null || targetBitArray.length == 0) {
            log.error("targetBitArray가 비어 있음");
            return results;
        }

        MinHash minHash = new MinHash(500, targetBitArray.length * 8, 123456);
        int[] signature1 = minHash.signature(byteArrayToBooleanArray(targetBitArray));

        for (String key : minHashMap.keySet()) {
            Double similarity = minHash.similarity(signature1, minHashMap.get(key));

            if (similarity != 0.0) {
                results.put(key, similarity);
            }
        }

        return results;
    }
}
