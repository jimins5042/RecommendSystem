package shop.RecommendSystem.recommend.ImageFeature;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import shop.RecommendSystem.dto.ImageFeatureApiDto;


@Slf4j
@Component
@RequiredArgsConstructor
public class ImageFeature {

    private final EfficientnetApiInterface efficientnetApiInterface;
    private final VGG16ApiInterface vgg16ApiInterface;

    /**
     * - VGG16 모델을 이용해 이미지의 특징점을 추출한 후, 0을 제외한 특징점 값들의 평균을 기준으로 이진화한 binary 값을 생성
     * - 레이어의 Intensity 평균값을 내림차순으로 정렬한 다음, 상위 25개의 레이어의 순서를 json 형태의 문자열로 변환
     *
     * @param file : 이미지 파일을 전달
     * @return : { features : " 이미지의 특징점 ",
     * order : " 상위 25개의 레이어의 순서 "}
     */
    public ImageFeatureApiDto sendImageToFastAPI(MultipartFile file, String model) throws Exception {
        ImageFeatureApiDto response = new ImageFeatureApiDto();

        if(model != null && !model.isEmpty()){
            if("efficientnet".equals(model)){
                response= efficientnetApiInterface.sendFile(file);
            }
            if("vgg16".equals(model)){
                response= vgg16ApiInterface.sendFile(file);
            }
        }

        return response;
    }

    public ImageFeatureApiDto sendCropImageToFastAPI(MultipartFile file, String model) throws Exception {
        ImageFeatureApiDto response = new ImageFeatureApiDto();

        if(model != null && !model.isEmpty()){
            if("efficientnet".equals(model)){
                response= efficientnetApiInterface.sendCropFile(file);
            }
            if("vgg16".equals(model)){
                response= vgg16ApiInterface.sendCropFile(file);
            }
        }
        return response;
    }

}
