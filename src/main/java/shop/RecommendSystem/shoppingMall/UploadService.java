package shop.RecommendSystem.shoppingMall;

import com.amazonaws.AmazonServiceException;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import shop.RecommendSystem.dto.ImageInfo;
import shop.RecommendSystem.recommend.ImageFeature.PHash;
import shop.RecommendSystem.repository.mapper.ItemMapper;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class UploadService {

    private final AmazonS3 amazonS3;
    private final ItemMapper itemMapper;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloudfront}")
    private String cloudfront;

    //aws s3에 이미지 업로드
    public String uploadFile(MultipartFile file) throws IOException {

        String uuid = UUID.randomUUID().toString();
        uuid = "contents/" + uuid;

        ObjectMetadata objMeta = new ObjectMetadata();
        objMeta.setContentLength(file.getInputStream().available());
        objMeta.setContentType(file.getContentType());

        try {
            amazonS3.putObject(bucket, uuid, file.getInputStream(), objMeta);

            // 등록된 객체의 url 반환 (decoder: url 안의 한글or특수문자 깨짐 방지)
            String imgUrl = (cloudfront + uuid).toString();

            String pHash = new PHash().getPHash(imgUrl);

            ImageInfo image = new ImageInfo(uuid, file.getOriginalFilename(), imgUrl, pHash);

            log.info("fullPath={} \nuuid={} \nfileName = {} \npHash = {}", imgUrl, uuid, file.getOriginalFilename(), pHash);
            itemMapper.insertImageInfo(image); //이미지 주소, 이름 저장


        } catch (AmazonServiceException e) {
            log.info("=== 이미지 저장 중 에러 발생)");
            e.printStackTrace();
        }
        return uuid;
    }

    //aws s3에 이미지 삭제
    public void deleteImage(String id) {

        try {
            // deleteObject(버킷명, 키값)으로 객체 삭제
            //amazonS3.deleteObject(bucket, itemMapper.findImageByUuid(id));

            //log.info("deleteImage={}", itemMapper.findImageByUuid(id));
            //itemMapper.deleteImage(id);    // db에 저장된 이미지 정보 삭제

        } catch (AmazonServiceException e) {
            log.error(e.toString());
        }


    }
}
