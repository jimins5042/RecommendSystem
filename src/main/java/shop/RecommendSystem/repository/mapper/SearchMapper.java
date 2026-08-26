package shop.RecommendSystem.repository.mapper;

import org.apache.ibatis.annotations.MapKey;
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
public interface SearchMapper {

    // ── ResNet-50 + PQ ──
    List<PqEntry> findAllPqCodes();

    List<SearchResult> findResnet50Phase2Targets(@Param("keySet") List<String> keySet, @Param("id") Long id, @Param("probes") int[] probes);

    // ── ResNet-50 완전탐색(brute-force) 평가용 ──
    // 전체 아이템의 fp16 임베딩 + 상품 정보. BruteForceSearch 가 1회 로드하여 메모리 캐시.
    List<SearchResult> findAllResnet50Embeddings();

}
