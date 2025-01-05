package shop.RecommendSystem.service;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.imgscalr.Scalr;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.Base64;

@Service
@Slf4j
public class ImageControlLogicService {

    public String ResizeImage1(String imageUrl, int targetWidth, int targetHeight) throws Exception {
        // S3 URL에서 이미지 다운로드
        URL url = new URL(imageUrl);
        BufferedImage originalImage = ImageIO.read(url);

        // 리사이즈 후 Base64로 변환
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Thumbnails.of(originalImage)
                .size(targetWidth, targetHeight)
                .outputFormat("jpeg")
                .outputQuality(0.8)
                .toOutputStream(outputStream);

        // Base64로 변환
        byte[] resizedImageData = outputStream.toByteArray();
        String base64Image = Base64.getEncoder().encodeToString(resizedImageData);

        // "data:image/jpeg;base64," 형식으로 반환
        return "data:image/jpeg;base64," + base64Image;
    }
    public String cropAndResizeImage(String imageUrl, int targetWidth, int targetHeight,int resizeType) throws Exception {
        // S3 URL에서 이미지 다운로드
        URL url = new URL(imageUrl);
        BufferedImage originalImage = ImageIO.read(url);

        Scalr.Method m;
        if(resizeType == 0) {
            m = Scalr.Method.QUALITY;
        }else if(resizeType == 1) {
            m = Scalr.Method.SPEED;
        }else if(resizeType == 2) {
            m = Scalr.Method.BALANCED;
        }else {
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

}
