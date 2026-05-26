package shop.RecommendSystem.recommend.ItemFiltering;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import shop.RecommendSystem.dto.PqEntry;
import shop.RecommendSystem.dto.PreFilterDto;
import shop.RecommendSystem.repository.mapper.SearchMapper;

import java.util.ArrayList;
import java.util.List;

/*
    검색 데이터 전처리용 클래스
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InitializeSearchData {

    public static final String PQ_FILTERING_KEY = "pqFiltering";
    public static final String PQ_FILTERING_FIELD = "searchData";

    private final RedisTemplate redisTemplate;
    private final SparseFeatureIndexing sparseFeatureIndexing;
    private final PQFiltering pqFiltering;
    private final SearchMapper searchMapper;

    // 프로젝트 시작시 실행
    @PostConstruct
    public void initializeSearchData() throws Exception {

        HashOperations<String, String, Object> hashOps = redisTemplate.opsForHash();

        // ── 1. BitwiseAndFiltering (기존 VGG16+AND 검색용) ─────────────
        ArrayList<PreFilterDto> bitwiseANDFilteringList = (ArrayList<PreFilterDto>) hashOps.get("bitwiseANDFiltering", "searchData");

//        if(true) {
        if(bitwiseANDFilteringList == null || bitwiseANDFilteringList.isEmpty()) {

            bitwiseANDFilteringList= sparseFeatureIndexing.initializeSearchData();
            hashOps.put("bitwiseANDFiltering", "searchData", bitwiseANDFilteringList);

            log.info("=== BitwiseAndFiltering list redis 적재 ===");
        }
        log.info("=== BitwiseAndFiltering list initialized ===");

        // ── 2. PQFiltering (신규 ResNet50+PQ 검색용) ───────────────────
        // Redis 캐시 hit 시 DB 풀스캔 회피. 데이터 변경 시(/importResnet50 등)
        // 캐시 무효화 후 재기동 필요. PQ_FILTERING_KEY 상수로 외부에서 invalidate 가능.
        @SuppressWarnings("unchecked")
        ArrayList<PqEntry> pqIndex = (ArrayList<PqEntry>) hashOps.get(PQ_FILTERING_KEY, PQ_FILTERING_FIELD);

        if (pqIndex == null || pqIndex.isEmpty()) {
            long t0 = System.currentTimeMillis();
            List<PqEntry> fromDb = searchMapper.findAllPqCodes();
            pqIndex = new ArrayList<>(fromDb);
            hashOps.put(PQ_FILTERING_KEY, PQ_FILTERING_FIELD, pqIndex);
            log.info("=== PQFiltering list DB → Redis 적재 ({}건, {}ms) ===",
                    pqIndex.size(), System.currentTimeMillis() - t0);
        } else {
            log.info("=== PQFiltering list Redis cache hit ({}건) ===", pqIndex.size());
        }

        // PQFiltering 메모리 구조에 주입 (Phase 1 검색용)
        pqFiltering.loadIndex(pqIndex);

    }

}
