package shop.RecommendSystem.shoppingMall;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import shop.RecommendSystem.MessageQueue.RedisService;
import shop.RecommendSystem.dto.ImageInfo;
import shop.RecommendSystem.recommend.ImageFeature.PHash;
import shop.RecommendSystem.repository.mapper.ItemMapper;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@RequiredArgsConstructor
@Service
public class UploadService {

    private final ItemMapper itemMapper;
    private final RedisService redisService;


    @Value("${resourcePath}")
    private String resourcePath;

    @Value("${connectPath}")
    private String connectPath;


    //로컬에 이미지 업로드
    public boolean uploadFile(MultipartFile[] files, Long itemId) throws IOException {


        try {
            for (MultipartFile file : files) {

                long uniqueKey = System.currentTimeMillis() * 1000 + ThreadLocalRandom.current().nextInt(1000);
                String uuid = String.valueOf(uniqueKey);

                String originalFilename = file.getOriginalFilename();

                String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();

                String fileName = uuid + "." + extension;

                File dir = new File(resourcePath);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                String imgUrl = connectPath + "/" + fileName;
                String pHash = new PHash().getPHash(file);

                File saveFile = new File(dir, fileName);
                file.transferTo(saveFile);

                ImageInfo image = ImageInfo.builder()
                        .imageUuid(uuid)                                //이미지 식별번호
                        .imageOriginalName(file.getOriginalFilename())  //이미지 명
                        .imageUrl(imgUrl)                               //이미지 저장경로
                        .imageHashCode(pHash)                           //이미지 pHash
                        .itemId(itemId)                                 //상품 번호
                        .build();

                log.info("fullPath={} \nuuid={} \nfileName = {} \npHash = {}", imgUrl, uuid, file.getOriginalFilename(), pHash);
                itemMapper.insertImageInfo(image); //이미지 주소, 이름 저장

                // redis stream(메시지큐)에 이미지 특징점 추출 요청 적재
                //redisService.sendToStream("imgSearch:preprocess_wait", Map.of("imageUuid", uuid, "imageUrl", imgUrl));
            }

        } catch (Exception e) {
            log.info("=== 이미지 저장 중 에러 발생)");
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
