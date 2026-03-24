package shop.RecommendSystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class ImageInfo {

    private Long imageId;
    private Long itemId;
    private String imageUuid;
    private String imageOriginalName;
    private String imageUrl;
    private String imageHashCode;
    private byte[] imgFeatureValue;
    private String imgFeatureOrder;
    private Date createdAt;
    private Date updatedAt;

    public ImageInfo() {
    }
}

