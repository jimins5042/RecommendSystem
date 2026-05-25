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

    // ── ResNet-50 + PQ 응답 필드 ──
    private String embeddingBase64;  // fp16 × 2048 (Phase 2 코사인 재랭킹용)
    private byte[] embedding;        // 위 base64 디코딩 결과
    private String pqCodeBase64;     // raw 64 byte (현재 사용 안 함, 디버깅용)
    private byte[] pqCode;

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

    public void setEmbeddingBase64(String embeddingBase64) {
        this.embeddingBase64 = embeddingBase64;
        if (embeddingBase64 != null) {
            this.embedding = Base64.getDecoder().decode(embeddingBase64);
        }
    }

    public void setPqCodeBase64(String pqCodeBase64) {
        this.pqCodeBase64 = pqCodeBase64;
        if (pqCodeBase64 != null) {
            this.pqCode = Base64.getDecoder().decode(pqCodeBase64);
        }
    }
}
