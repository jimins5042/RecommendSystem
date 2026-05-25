package shop.RecommendSystem.recommend.ImageFeature;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import shop.RecommendSystem.dto.ImageFeatureApiDto;

@FeignClient(
        name = "Resnet50FileServerSend",
        url = "${fastApiUrl}"
)
public interface ResNet50ApiInterface {
    @PostMapping(value = "/api/resnet50/process-image/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ImageFeatureApiDto sendFile(@RequestPart("file") MultipartFile file);

    @PostMapping(value = "/api/resnet50/process-image/crop/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ImageFeatureApiDto sendCropFile(@RequestPart("file") MultipartFile file);
}
