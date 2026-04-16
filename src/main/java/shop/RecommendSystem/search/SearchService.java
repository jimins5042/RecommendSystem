package shop.RecommendSystem.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import shop.RecommendSystem.dto.SearchResult;
import shop.RecommendSystem.recommend.ItemFiltering.BitwiseAndFiltering;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final BitwiseAndFiltering filter;

    public List<SearchResult> searchSimilarItems(String order, byte[] feature, int resultSize, Long id) throws JsonProcessingException {

        return filter.searchSimilarItem(order, feature, resultSize, id);
    }

}
