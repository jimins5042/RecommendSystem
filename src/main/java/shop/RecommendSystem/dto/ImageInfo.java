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

    private Long itemId;
    private String imageUuid;
    private String imageOriginalName;
    private String imageUrl;
    private String imageHashCode;
    private byte[] imgFeatureValue;
    private String imgFeatureOrder;
    private Date createdAt;
    private Date updatedAt;

    // ResNet-50 + PQ (신규)
    private byte[] pqCode;          // raw 64 byte
    private byte[] embeddingValue;  // fp16 × 2048 = 4,096 byte
    private String detectedClass;   // YOLO 분류 (bag/shoes/clothing/sunglasses/food_drink)

    public ImageInfo() {
    }
}

