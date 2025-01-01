package shop.RecommendSystem.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter@Setter
public class ImageInfo {

    int imageId;
    String imageUuid;
    String imageOriginalName;
    String imageUrl;
    String imageHashCode;
    String imagePhashV1;
    Date createdAt;
    Date updatedAt;

    public ImageInfo() {
    }

    public ImageInfo(String imageUuid, String imageOriginalName, String imageUrl, String imageHashCode) {
        this.imageUuid = imageUuid;
        this.imageOriginalName = imageOriginalName;
        this.imageUrl = imageUrl;
        this.imageHashCode = imageHashCode;
    }

    public ImageInfo(int imageId, String imageUuid, String imageOriginalName, String imageUrl, String imageHashCode, String imagePhashV1, Date createdAt, Date updatedAt) {
        this.imageId = imageId;
        this.imageUuid = imageUuid;
        this.imageOriginalName = imageOriginalName;
        this.imageUrl = imageUrl;
        this.imageHashCode = imageHashCode;
        this.imagePhashV1 = imagePhashV1;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}

