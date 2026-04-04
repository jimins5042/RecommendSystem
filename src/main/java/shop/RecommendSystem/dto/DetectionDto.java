package shop.RecommendSystem.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter
@NoArgsConstructor
public class DetectionDto {
    @JsonProperty("class")
    private String className;    //"class"는 예약어라 필드명 주의
    private double confidence;
    private String[] coordinate;

    public DetectionDto(String className, double confidence, String[] coordinate) {
        this.className = className;
        this.confidence = confidence;
        this.coordinate = coordinate;
    }
}
