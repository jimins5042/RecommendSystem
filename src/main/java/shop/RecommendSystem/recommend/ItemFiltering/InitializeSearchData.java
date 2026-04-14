package shop.RecommendSystem.recommend.ItemFiltering;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import shop.RecommendSystem.dto.ImageInfo;
import shop.RecommendSystem.dto.PreFilterDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/*
    검색 데이터 전처리용 클래스
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InitializeSearchData {

    private final RedisTemplate redisTemplate;
    private final BitwiseAndFiltering bitwiseAndFiltering;
    private final PrefixFiltering prefixFiltering;
    private final MinHashFiltering minHashFiltering;

    // 프로젝트 시작시 실행
    @PostConstruct
    public void initializeSearchData() throws Exception {

        HashOperations<String, String, Object> hashOps = redisTemplate.opsForHash();

        ArrayList<PreFilterDto> bitwiseANDFilteringList = (ArrayList<PreFilterDto>) hashOps.get("bitwiseANDFiltering", "searchData");
        bitwiseANDFilteringList = bitwiseAndFiltering.initializeSearchData();
        hashOps.put("bitwiseANDFiltering", "searchData", bitwiseANDFilteringList);

        log.info("=== BitwiseAndFiltering list redis 적재 ===");
//        if(bitwiseANDFilteringList == null || bitwiseANDFilteringList.isEmpty()) {
//            bitwiseANDFilteringList= bitwiseAndFiltering.initializeSearchData();
//            hashOps.put("bitwiseANDFiltering", "searchData", bitwiseANDFilteringList);
//
//            log.info("=== BitwiseAndFiltering list redis 적재 ===");
//        }
        log.info("=== BitwiseAndFiltering list initialized ===");

//        HashMap<String, HashSet<ImageInfo>> prefixFilteringHash = (HashMap<String, HashSet<ImageInfo>>) hashOps.get("prefixFiltering", "searchData");
//
//        if(prefixFilteringHash == null || prefixFilteringHash.isEmpty()) {
//            prefixFilteringHash = prefixFiltering.initializeSearchData();
//            hashOps.put("prefixFiltering", "searchData", prefixFilteringHash);
//        }
//
//        HashMap<String, int[]> minHashFilteringHash = (HashMap<String, int[]>) hashOps.get("minHashFiltering", "searchData");
//
//        if(minHashFilteringHash == null || minHashFilteringHash.isEmpty()) {
//            minHashFilteringHash =  minHashFiltering.initializeSearchData();
//            hashOps.put("minHashFiltering", "searchData", minHashFilteringHash);
//        }

    }

}
