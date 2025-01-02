package shop.RecommendSystem.service;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.Base64;

@Service
@Slf4j
public class ImageControlLogicService {

    public String cropAndResizeImage(String imageUrl, int targetWidth, int targetHeight) throws Exception {
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


}
