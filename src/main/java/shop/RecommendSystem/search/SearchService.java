package shop.RecommendSystem.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import shop.RecommendSystem.dto.ImageFeatureApiDto;
import shop.RecommendSystem.dto.ItemFilteringVo;
import shop.RecommendSystem.dto.SearchResult;
import shop.RecommendSystem.recommend.ItemFiltering.ItemFiltering;
import shop.RecommendSystem.recommend.ItemFiltering.PQFiltering;
import shop.RecommendSystem.recommend.ItemFiltering.SparseFeatureIndexing;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    @Qualifier("resnet")
    private final ItemFiltering itemFiltering;

    private final PQFiltering pqFiltering;
    private final SparseFeatureIndexing sparseFeatureIndexing;

    public List<SearchResult> searchSimilarItems(ItemFilteringVo searchParam, int resultSize, Long id) {
        return itemFiltering.searchSimilarItem(searchParam, resultSize, id);
    }


    public List<SearchResult> searchBranchByBackbone(String backbone, ImageFeatureApiDto apiResult){

        List<SearchResult> results;
        if ("resnet50".equals(backbone)) {

            results = pqFiltering.searchSimilarItem(
                    new ItemFilteringVo().pqFiltering(apiResult.getEmbedding()),
                    20,
                    null);

        } else if ("vggnet".equals(backbone)) {

            results = sparseFeatureIndexing.searchSimilarItem(
                    new ItemFilteringVo().sparseFeatureIndexing(apiResult.getFeatures(), apiResult.getOrder()),
                    20,
                    null);
        } else {
            return null;
        }

        return results;
    }

//    public List<SearchResult> searchSimilarItems(String order, byte[] feature, int resultSize, Long id) throws JsonProcessingException {
//
//        return filter.searchSimilarItem(order, feature, resultSize, id);
//    }
//
//    /**
//     * ResNet-50 + PQ 검색 — A/B 비교용 신규 경로.
//     *
//     * @param embeddingBytes fp16 × 2048 = 4,096 byte (FastAPI 응답)
//     * @param classFilter    YOLO detected_class 필터. null 이면 전체 검색
//     * @param resultSize     최종 반환 개수
//     * @param excludeItemId  본인 상품 제외 (null 허용)
//     */
//    public List<SearchResult> searchByResnet50(byte[] embeddingBytes, String classFilter, int resultSize, Long excludeItemId) {
//        float[] queryEmbedding = PQFiltering.decodeFp16(embeddingBytes);
//        return pqFiltering.searchSimilarItem(queryEmbedding, classFilter, resultSize, excludeItemId);
//    }

}
