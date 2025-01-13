package shop.RecommendSystem.repository.mapper;

import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import shop.RecommendSystem.dto.ImageInfo;
import shop.RecommendSystem.dto.SearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mapper
public interface SearchMapper {
    ArrayList<ImageInfo> findSearchTarget();

    List<SearchResult> findItemCandidates(List<String> keySet);


    Map<String, String> findImageColorTags(List<String> imageColorUuids);

    String findImageColorTag(String imageColorUuid);
    String findImageColorTagByuuid(String imageColorUuid);

}
