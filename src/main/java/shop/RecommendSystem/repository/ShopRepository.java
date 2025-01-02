package shop.RecommendSystem.repository;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Repository;
import shop.RecommendSystem.dto.Item;
import shop.RecommendSystem.repository.mapper.ItemMapper;
import shop.RecommendSystem.service.ImageControlLogicService;
import shop.RecommendSystem.service.ImagePHash;
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
    private final ImageControlLogicService imgctrl;


    public long save(Item item) {
        log.info("매퍼={}", item.getItemTitle());
        itemMapper.save(item); // 삽입 실행

        log.info("itemId={}", item.getItemId());

        return item.getItemId(); // 자동 생성된 itemId 반환
    }

    public Item findById(Long itemId) {
        Item item = itemMapper.findById(itemId);

        try{
            item.setItemImageLink(imgctrl.cropAndResizeImage(item.getItemImageLink(), 400, 300));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return item;
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

        return items;
    }

    public Long countItems() {
        return itemMapper.countItems();
    }

    public void deleteItem(Long id) {
        itemMapper.delete(id);
    }
}
