package shop.RecommendSystem.dto;


import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter@Setter
public class ImageFeaturesResponse {
    private List<List<Float>> descriptors;

    public ImageFeaturesResponse() {
    }

    public ImageFeaturesResponse(List<List<Float>> descriptors) {
        this.descriptors = descriptors;
    }
}