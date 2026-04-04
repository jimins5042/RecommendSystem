package shop.RecommendSystem.shoppingMall;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import shop.RecommendSystem.MessageQueue.RedisService;
import shop.RecommendSystem.dto.ImageInfo;
import shop.RecommendSystem.dto.Item;
import shop.RecommendSystem.recommend.ImageFeature.PHash;
import shop.RecommendSystem.repository.mapper.ItemMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

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

    private static final AtomicLong counter = new AtomicLong(0);


    //다중 이미지 저장
    public boolean uploadFile(MultipartFile[] files, Long itemId) throws IOException {

        try {

            List<ImageInfo> imageInfoList = new ArrayList<>();

            for (MultipartFile file : files) {
                ImageInfo imageInfo = getFileInfo(file, itemId);
                imageInfoList.add(imageInfo);
            }

            Long isSuccess= itemMapper.insertBulkImageInfo(imageInfoList);
            if(isSuccess <= 0){
                return false;
            }

        } catch (Exception e) {
            log.info("=== 이미지 저장 중 에러 발생)");
            e.printStackTrace();
            return false;
        }

        return true;
    }

    // 단일 이미지 파일 저장
    public boolean uploadFile(MultipartFile file, Long itemId) throws IOException {

        try {
            ImageInfo imageInfo = getFileInfo(file, itemId);
            Long isSuccess= itemMapper.insertImageInfo(imageInfo); //이미지 주소, 이름 저장
            if(isSuccess <= 0){
                return false;
            }

            // redis stream(메시지큐)에 이미지 특징점 추출 요청 적재
            //redisService.sendToStream("imgSearch:preprocess_wait", Map.of("imageUuid", uuid, "imageUrl", imgUrl));

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // DB 저장 없이 파일복사 + pHash만 수행 (카테고리별 폴더 저장)
    public ImageInfo processFile(MultipartFile file, Long imageId, Long itemId, String category) throws IOException {
        long uniqueKey = System.currentTimeMillis() * 10000 + counter.getAndIncrement() % 10000;
        String uuid = String.valueOf(uniqueKey);

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        String fileName = uuid + "." + extension;

        // 카테고리 하위 폴더에 저장
        File dir = new File(resourcePath, category);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String imgUrl = connectPath + "/" + category + "/" + fileName;
        String pHash = new PHash().getPHash(file);

        File saveFile = new File(dir, fileName);
        file.transferTo(saveFile);

        ImageInfo image = ImageInfo.builder()
                .imageUuid(uuid)
                .imageOriginalName(file.getOriginalFilename())
                .imageUrl(imgUrl)
                .imageHashCode(pHash)
                .itemId(itemId)
                .build();

        log.info("fullPath={} \nuuid={} \nfileName={} \npHash={}", imgUrl, uuid, file.getOriginalFilename(), pHash);
        return image;
    }

    public ImageInfo getFileInfo(MultipartFile file, Long itemId) throws IOException {

        try {
            long uniqueKey = System.currentTimeMillis() * 10000 + counter.getAndIncrement() % 10000;
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
            return image;

        } catch (Exception e) {
            log.error("이미지 처리 실패 - 파일: {}, 원인: {}", file.getOriginalFilename(), e.getMessage(), e);
            throw new IOException("이미지 처리 실패: " + file.getOriginalFilename(), e);
        }
    }


}
