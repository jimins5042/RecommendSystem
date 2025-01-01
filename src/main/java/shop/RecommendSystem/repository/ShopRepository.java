package shop.RecommendSystem.repository;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import shop.RecommendSystem.dto.Item;
import shop.RecommendSystem.repository.mapper.ItemMapper;

@Repository
@Getter
@Setter
@Slf4j
@RequiredArgsConstructor
public class ShopRepository {

    //mapper의 의존관계 구현체를 주입
    private final ItemMapper itemMapper;

    public long save(Item item) {
        log.info("매퍼={}",item.getItemTitle());
        itemMapper.save(item); // 삽입 실행

        log.info("itemId={}",item.getItemId());

        return item.getItemId(); // 자동 생성된 itemId 반환
    }


    public Item findById(Long itemId) {
        return itemMapper.findById(itemId);
    }
    public void deleteItem(Long id) {
        itemMapper.delete(id);
    }
}
