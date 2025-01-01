package shop.RecommendSystem.repository.mapper;

import org.apache.ibatis.annotations.Mapper;
import shop.RecommendSystem.dto.ImageInfo;
import shop.RecommendSystem.dto.Item;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ItemMapper {

    void save(Item item);
    void insertImageInfo(ImageInfo imageInfo);

    Item findById(Long id);

    List<Item> findAll(Item itemSearch);

    void delete(Long id);
}