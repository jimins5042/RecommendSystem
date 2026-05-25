package shop.RecommendSystem.repository.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import shop.RecommendSystem.dto.ImageInfo;
import shop.RecommendSystem.dto.PqEntry;
import shop.RecommendSystem.dto.PreFilterDto;
import shop.RecommendSystem.dto.SearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mapper
public interface SearchMapperVggnet {

    ArrayList<PreFilterDto> findReduceTarget();

    ArrayList<PreFilterDto> findPreFilterTarget(List<String> keySet);

    List<SearchResult> findPreFilterTargetV2(@Param("keySet") List<String> keySet, @Param("id")Long id);


}
