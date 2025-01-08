package shop.RecommendSystem.dto;

import lombok.Getter;
import lombok.Setter;

@Getter@Setter
public class SearchResult {

    private Long itemId;
    private String itemTitle;
    private String itemPrice;
    private String imageUrl;
    private String imageUuid;
    private Double hammingDistance;

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
}
