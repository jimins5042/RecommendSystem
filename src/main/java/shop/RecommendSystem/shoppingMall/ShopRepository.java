package shop.RecommendSystem.shoppingMall;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import shop.RecommendSystem.dto.Item;
import shop.RecommendSystem.repository.mapper.ItemMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
        List<Item> itemList = itemMapper.findById(itemId);
        if(itemList == null || itemList.isEmpty()) {
            return null;
        }

        Item item = itemList.get(0);

        if(itemList.size() > 1) {
            List<String> imgaeList = itemList.stream().map(Item::getImageUrl).toList();
            item.setImageUrlList(imgaeList);
        }

        return item;
    }

    public List<Item> selectAll(Long page, Long size) {
        if (page == null) {
            page = 1L;
        }
        if (size == null) {
            size = 10L;
        }
        log.info("offset: " + page);
        Map<String, Long> map = new HashMap<>();
        map.put("offset", page);
        map.put("size", size);
        return itemMapper.findAll(map);
    }

    public List<Item> findThumbnailAll(Long page, Long size, String category) throws Exception {
        if (page == null) {
            page = 1L;
        }
        if (size == null) {
            size = 10L;
        }
        Map<String, Object> map = new HashMap<>();
        map.put("offset", page);
        map.put("size", size);
        map.put("category", category);

        List<Item> items = itemMapper.findThumbnailAll(map);

        return items;
    }

    public Long countItems(String category) {
        return itemMapper.countItems(Map.of("category", category));
    }

    public void deleteItem(Long id) {
        itemMapper.delete(id);
    }
}
