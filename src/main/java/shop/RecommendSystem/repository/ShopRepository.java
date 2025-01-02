package shop.RecommendSystem.repository;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Repository;
import shop.RecommendSystem.dto.Item;
import shop.RecommendSystem.repository.mapper.ItemMapper;
import shop.RecommendSystem.service.ShopService;

import java.util.Base64;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Repository
@Getter
@Setter
@Slf4j
@RequiredArgsConstructor
public class ShopRepository {

    //mapper의 의존관계 구현체를 주입
    private final ItemMapper itemMapper;


    public long save(Item item) {
        log.info("매퍼={}", item.getItemTitle());
        itemMapper.save(item); // 삽입 실행

        log.info("itemId={}", item.getItemId());

        return item.getItemId(); // 자동 생성된 itemId 반환
    }

    public Item findById(Long itemId) {
        return itemMapper.findById(itemId);
    }

    public List<Item> selectAll(Long page, Long size) {
        if (page == null || page < 1) {
            page = 1L;
        }
        if (size == null || size < 1) {
            size = 10L;
        }
        Map<String, Long> map = new HashMap<>();
        map.put("offset", page);
        map.put("size", size);
        return itemMapper.findAll(map);
    }

    public List<Item> findThumbnailAll(Long page, Long size) throws Exception {
        if (page == null || page < 1) {
            page = 1L;
        }
        if (size == null || size < 1) {
            size = 10L;
        }
        Map<String, Long> map = new HashMap<>();
        map.put("offset", page);
        map.put("size", size);

        List<Item> items = itemMapper.findThumbnailAll(map);
/*
        for (Item item : items) {
            String url = cropAndResizeImage(item.getItemImageLink(), 400, 300);

            item.setItemImageLink(url);
        }

 */     //비동기 처리
        ExecutorService executor = Executors.newFixedThreadPool(10); // 최대 10개의 스레드를 사용
        List<CompletableFuture<Void>> futures = items.stream()
                .map(item -> CompletableFuture.runAsync(() -> {
                    try {
                        String url = cropAndResizeImage(item.getItemImageLink(), 400, 300);
                        item.setItemImageLink(url);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, executor))  // Executor를 명시적으로 지정
                .collect(Collectors.toList());

        // 모든 작업이 완료될 때까지 기다림
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        executor.shutdown(); // 작업 완료 후 Executor 종료

        return items;
    }

    private String cropAndResizeImage(String imageUrl, int targetWidth, int targetHeight) throws Exception {
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

    public Long countItems() {
        return itemMapper.countItems();
    }

    public void deleteItem(Long id) {
        itemMapper.delete(id);
    }
}
