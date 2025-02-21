package shop.RecommendSystem.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter@Setter@Builder
public class PreFilterDto {

    String imageUuid;
    String featureOrder;
    byte[] layerOrderList;
    byte[] imgFeatureValue;

    public PreFilterDto() {
    }

    public PreFilterDto(String imageUuid, String featureOrder) {
        this.imageUuid = imageUuid;
        this.featureOrder = featureOrder;
    }

    public PreFilterDto(String imageUuid, String featureOrder, byte[] layerOrderList) {
        this.imageUuid = imageUuid;
        this.featureOrder = featureOrder;
        this.layerOrderList = layerOrderList;
    }

    public PreFilterDto(String imageUuid, String featureOrder, byte[] layerOrderList, byte[] imgFeatureValue) {
        this.imageUuid = imageUuid;
        this.featureOrder = featureOrder;
        this.layerOrderList = layerOrderList;
        this.imgFeatureValue = imgFeatureValue;
    }
}
