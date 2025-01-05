package shop.RecommendSystem.service;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.imgscalr.Scalr;
import shop.RecommendSystem.dto.ColorTag;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Objects;

@Service
@Slf4j
public class ImageControlLogicService {

    public String cropAndResizeImage(String imageUrl, int targetWidth, int targetHeight, int resizeType) throws Exception {
        // S3 URL에서 이미지 다운로드
        URL url = new URL(imageUrl);
        BufferedImage originalImage = ImageIO.read(url);

        Scalr.Method m;
        if (resizeType == 0) {
            m = Scalr.Method.QUALITY;
        } else if (resizeType == 1) {
            m = Scalr.Method.SPEED;
        } else if (resizeType == 2) {
            m = Scalr.Method.BALANCED;
        } else {
            m = Scalr.Method.AUTOMATIC;
        }

        // Imgscalr를 사용하여 리사이징
        BufferedImage resizedImage = Scalr.resize(
                originalImage,
                m,  // 지정한 품질로 리사이징
                Scalr.Mode.AUTOMATIC,      // 비율 유지
                targetWidth, targetHeight
        );

        // 리사이징된 이미지를 ByteArray로 변환
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, "jpeg", outputStream);
        byte[] resizedImageData = outputStream.toByteArray();

        // Base64로 인코딩
        String base64Image = Base64.getEncoder().encodeToString(resizedImageData);

        // "data:image/jpeg;base64," 형식으로 반환
        return "data:image/jpeg;base64," + base64Image;
    }

    public String getNearestColor(int[] rgb) {

        double mean = (rgb[0] + rgb[1] + rgb[2]) / 3;

        //rgb 값 표준 편차
        double sStdDev = Math.sqrt((
                Math.pow(rgb[0] - mean, 2)
                        + Math.pow(rgb[1] - mean, 2)
                        + Math.pow(rgb[2] - mean, 2)
        ) / 3);

        if (sStdDev < 10) {
            if (mean < 20) {
                return ColorTag.WHITE.name();
            }

            if (mean > 235) {
                return ColorTag.BLACK.name();
            }

            return ColorTag.GRAY.name();
        }

        String findColor = "";

        double minDist = Double.MAX_VALUE;
        for (ColorTag color : ColorTag.values()) {
            int[] colorValue = color.getRgb();
            double curDist = Math.sqrt(
                    Math.pow(rgb[0] - colorValue[0], 2) + // Red 차이
                            Math.pow(rgb[1] - colorValue[1], 2) + // Green 차이
                            Math.pow(rgb[2] - colorValue[2], 2)   // Blue 차이
            );

            if (minDist > curDist) {
                minDist = curDist;
                findColor = color.name(); // 색상 이름 반환 (예: "Red")
            }
        }
        return findColor;
    }
}
