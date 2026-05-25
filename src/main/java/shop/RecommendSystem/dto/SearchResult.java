package shop.RecommendSystem.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchResult {

    private Long itemId;
    private String itemTitle;
    private String itemPrice;
    private String imageUrl;
    private String imageUuid;
    private Double hammingDistance;
    private byte[] imgFeatureValue;

    // ── ResNet-50 + PQ Phase 2 용 ──
    private byte[] embeddingValue;       // fp16 × 2048 = 4,096 byte (DB 로드용)
    private Double cosineSimilarity;     // Phase 2 정밀 재랭킹 점수

    public SearchResult() {
    }

    public SearchResult(Long itemId, String itemTitle, String itemPrice, String imageUrl, String imageUuid, Double hammingDistance) {
        this.itemId = itemId;
        this.itemTitle = itemTitle;
        this.itemPrice = itemPrice;
        this.imageUrl = imageUrl;
        this.imageUuid = imageUuid;
        this.hammingDistance = hammingDistance;
    }

    public SearchResult(Long itemId, String itemTitle, String itemPrice, String imageUrl, String imageUuid, Double hammingDistance, byte[] imgFeatureValue) {
        this.itemId = itemId;
        this.itemTitle = itemTitle;
        this.itemPrice = itemPrice;
        this.imageUrl = imageUrl;
        this.imageUuid = imageUuid;
        this.hammingDistance = hammingDistance;
        this.imgFeatureValue = imgFeatureValue;
    }
}
