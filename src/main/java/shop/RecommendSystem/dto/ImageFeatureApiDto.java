package shop.RecommendSystem.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Base64;
import java.util.List;

@Getter
@Setter
public class ImageFeatureApiDto {

    private String order;           // fillter index 순서
    private String featuresBase64;  // 추출한 이미지 특징점을 base64로 인코딩한 것
    private byte[] features;        // 추출한 이미지 특징점
    private String[] coordinate;    // confidence가 가장 높은 객체의 좌표값
    private String detectedClass;   // confidence가 가장 높은 객체의 구분값
    private String confidence;      // confidence가 가장 높은 객체의 confidence 값
    private List<DetectionDto> detections; // 감지된 다른 객체들의 좌표값

    public ImageFeatureApiDto() {}

    public ImageFeatureApiDto(String order, String featuresBase64) {
        this.order = order;
        this.setFeaturesBase64(featuresBase64);
    }

    public ImageFeatureApiDto(String order, String featuresBase64, String[] coordinate, String detectedClass, String confidence, List<DetectionDto> detections) {
        this.order = order;
        this.setFeaturesBase64(featuresBase64);
        this.coordinate = coordinate;
        this.detectedClass = detectedClass;
        this.confidence = confidence;
        this.detections = detections;
    }

    // Jackson이 featuresBase64를 세팅할 때 features도 함께 디코딩
    public void setFeaturesBase64(String featuresBase64) {
        this.featuresBase64 = featuresBase64;
        if (featuresBase64 != null) {
            this.features = Base64.getDecoder().decode(featuresBase64);
        }
    }
}
