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

    public ImageInfo(String imageUuid, String imageHashCode) {
        this.imageUuid = imageUuid;
        this.imageHashCode = imageHashCode;
    }

    public ImageInfo(String imageUuid, String imageOriginalName, String imageUrl, String imageHashCode) {
        this.imageUuid = imageUuid;
        this.imageOriginalName = imageOriginalName;
        this.imageUrl = imageUrl;
        this.imageHashCode = imageHashCode;
    }

    public ImageInfo(Long imageId, String imageUuid, String imageOriginalName, String imageUrl, String imageHashCode, Date createdAt, Date updatedAt) {
        this.imageId = imageId;
        this.imageUuid = imageUuid;
        this.imageOriginalName = imageOriginalName;
        this.imageUrl = imageUrl;
        this.imageHashCode = imageHashCode;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}

