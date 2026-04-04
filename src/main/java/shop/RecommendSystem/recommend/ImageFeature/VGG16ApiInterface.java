package shop.RecommendSystem.recommend.ImageFeature;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import shop.RecommendSystem.dto.VGG16ApiDto;

import java.util.HashMap;
@FeignClient(
        name = "FileServerSend",
        url = "${fastApiUrl}"
)
public interface VGG16ApiInterface {
    @PostMapping(value = "/process-image/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    VGG16ApiDto sendFile(@RequestPart("file") MultipartFile file);

    @PostMapping(value = "/process-image/crop/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    VGG16ApiDto sendCropFile(@RequestPart("file") MultipartFile file);
}
