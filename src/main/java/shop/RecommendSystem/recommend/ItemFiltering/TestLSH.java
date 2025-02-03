package shop.RecommendSystem.recommend.ItemFiltering;

import info.debatty.java.lsh.MinHash;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import shop.RecommendSystem.dto.ImageInfo;
import shop.RecommendSystem.repository.mapper.SearchMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestLSH {
    private final SearchMapper searchMapper;

    private HashMap<String, int[]> bitmaskTree = new HashMap<>();

    @PostConstruct
    public void initializeBitMaskTree() {
        log.info("Initializing BitMask Tree...");

        ArrayList<ImageInfo> images = searchMapper.findSearchBitMaskTarget();

        if (images == null || images.isEmpty()) {
            log.warn("No images found for bitmask tree initialization.");
            return;
        }

        for (ImageInfo image : images) {
            String hashCode = image.getImageHashCode();
            if (hashCode == null || hashCode.isEmpty()) {
                log.warn("Skipping image with null or empty hash code: {}", image);
                continue;
            }

            TreeSet<Integer> set1 = binaryToSet(hashCode);
            if (set1.isEmpty()) {
                log.warn("Skipping image with empty binary feature set for hash code: {}", hashCode);
                continue;
            }

            try {
                MinHash minHash = new MinHash(500, set1.size(), 123456);
                int[] signature1 = minHash.signature(set1);
                bitmaskTree.put(image.getImageUuid(), signature1);
            } catch (Exception e) {
                log.error("Error initializing MinHash for hash code: {}", hashCode, e);
            }
        }

        log.info("BitMask Tree initialized with {} entries.", bitmaskTree.size());
    }


    public HashMap<String, Double> searchLsh(String value) {
        log.info("Searching for searchLsh...");
        HashMap<String, Double> results = new HashMap<>();

        TreeSet<Integer> set1 = binaryToSet(value);
        MinHash minHash = new MinHash(500, set1.size(), 123456);
        int[] signature1 = minHash.signature(set1);

        for (String key : bitmaskTree.keySet()) {
            Double similarity = minHash.similarity(signature1, bitmaskTree.get(key));

            if (similarity != 0.0) {
                results.put(key, similarity);
                log.info("Key : {}, Similarity: {}", key, similarity);
            }

        }

        return results;
    }


    private static TreeSet<Integer> binaryToSet(String binaryFeature) {
        TreeSet<Integer> set = new TreeSet<>();
        for (int i = 0; i < binaryFeature.length(); i++) {
            if (binaryFeature.charAt(i) == '1') {
                set.add(i); // 1의 위치를 추가
            }
        }
        return set;
    }
}
