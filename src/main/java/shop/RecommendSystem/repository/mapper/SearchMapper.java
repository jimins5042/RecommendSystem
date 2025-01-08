package shop.RecommendSystem.repository.mapper;

import org.apache.ibatis.annotations.Mapper;
import shop.RecommendSystem.dto.ImageInfo;
import shop.RecommendSystem.dto.SearchResult;

import java.util.ArrayList;
import java.util.List;

@Mapper
public interface SearchMapper {
    ArrayList<ImageInfo> findSearchTarget();
    List<SearchResult> findItemCandidates(List<String> keySet);
}
