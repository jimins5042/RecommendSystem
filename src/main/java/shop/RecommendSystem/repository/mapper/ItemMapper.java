package shop.RecommendSystem.repository.mapper;

import org.apache.ibatis.annotations.Mapper;
import shop.RecommendSystem.dto.ImageInfo;
import shop.RecommendSystem.dto.Item;

import java.util.List;
import java.util.Map;

@Mapper
public interface ItemMapper {

    void save(Item item);
    void insertImageInfo(ImageInfo imageInfo);

    Item findById(Long id);

    List<Item> findAll(Map<String, Long> map);
    List<Item> findThumbnailAll(Map<String, Long> map);

    Long countItems();

    void delete(Long id);
}