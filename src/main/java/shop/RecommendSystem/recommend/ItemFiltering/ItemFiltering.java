package shop.RecommendSystem.recommend.ItemFiltering;

import com.fasterxml.jackson.core.JsonProcessingException;
import shop.RecommendSystem.dto.Item;
import shop.RecommendSystem.dto.ItemFilteringVo;
import shop.RecommendSystem.dto.SearchResult;

import java.util.List;
import java.util.Map;

public interface ItemFiltering {
    List<SearchResult> searchSimilarItem(ItemFilteringVo searchParam, int resultSize, Long id);
}
