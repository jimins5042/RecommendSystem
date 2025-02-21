package shop.RecommendSystem.recommend.ImageFeature;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;

@Slf4j
public class VGG16 {

    private static final String FASTAPI_URL = "http://127.0.0.1:8000/process-image/";

    /**
     - VGG16 모델을 이용해 이미지의 특징점을 추출한 후, 0을 제외한 특징점 값들의 평균을 기준으로 이진화한 binary 값을 생성
     - 레이어의 Intensity 평균값을 내림차순으로 정렬한 다음, 상위 25개의 레이어의 순서를 json 형태의 문자열로 변환

     * @param file : 이미지 파일을 전달
     * @return : { features : " 이미지의 특징점 ",
                   order : " 상위 25개의 레이어의 순서 "}
     */
    public HashMap<String, Object> sendImageToFastAPI(MultipartFile file) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new HttpEntity<>(file.getBytes(), headers));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<HashMap> response = restTemplate.exchange(FASTAPI_URL, HttpMethod.POST, requestEntity, HashMap.class);

        return response.getBody();
    }

}
