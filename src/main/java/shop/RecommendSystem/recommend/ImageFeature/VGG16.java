package shop.RecommendSystem.recommend.ImageFeature;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import shop.RecommendSystem.dto.DetectionDto;
import shop.RecommendSystem.dto.VGG16ApiDto;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@Component
@RequiredArgsConstructor
public class VGG16 {

    //@Value("${fastApiUrl}")
    //private String url;

    private String url = "http://127.0.0.1:8000/process-image/";

    private final VGG16ApiInterface vgg16ApiInterface;

    /**
     * - VGG16 모델을 이용해 이미지의 특징점을 추출한 후, 0을 제외한 특징점 값들의 평균을 기준으로 이진화한 binary 값을 생성
     * - 레이어의 Intensity 평균값을 내림차순으로 정렬한 다음, 상위 25개의 레이어의 순서를 json 형태의 문자열로 변환
     *
     * @param file : 이미지 파일을 전달
     * @return : { features : " 이미지의 특징점 ",
     * order : " 상위 25개의 레이어의 순서 "}
     */
    public VGG16ApiDto sendImageToFastAPI(MultipartFile file) throws Exception {
        VGG16ApiDto response= vgg16ApiInterface.sendFile(file);
        return response;
    }

    public VGG16ApiDto sendCropImageToFastAPI(MultipartFile file) throws Exception {
        VGG16ApiDto response= vgg16ApiInterface.sendCropFile(file);
        return response;

    }

}
