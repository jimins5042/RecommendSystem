package shop.RecommendSystem.repository.mapper;

import org.apache.ibatis.annotations.Mapper;
import shop.RecommendSystem.dto.ImageInfo;

import java.util.ArrayList;

@Mapper
public interface SearchMapper {
    ArrayList<ImageInfo> findSearchTarget();
}
