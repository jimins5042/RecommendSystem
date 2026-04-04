package shop.RecommendSystem.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import shop.RecommendSystem.dto.DetectionDto;
import shop.RecommendSystem.dto.SearchResult;
import shop.RecommendSystem.dto.VGG16ApiDto;
import shop.RecommendSystem.recommend.ImageFeature.PHash;
import shop.RecommendSystem.recommend.ImageFeature.VGG16;
import shop.RecommendSystem.recommend.ItemFiltering.BitwiseAndFiltering;
import shop.RecommendSystem.recommend.ItemFiltering.MinHashFiltering;
import shop.RecommendSystem.recommend.ItemFiltering.PrefixFiltering;
import shop.RecommendSystem.repository.mapper.SearchMapper;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final BitwiseAndFiltering filter;
    private final VGG16 vgg16;


    public List<SearchResult> searchSimilarItems(String order, byte[] feature, int resultSize, Long id) throws JsonProcessingException {

        return filter.searchSimilarItem(order, feature, resultSize, id);
    }

}
