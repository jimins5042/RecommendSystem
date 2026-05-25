package shop.RecommendSystem.search;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import shop.RecommendSystem.dto.DetectionDto;
import shop.RecommendSystem.dto.Item;
import shop.RecommendSystem.dto.SearchResult;
import shop.RecommendSystem.dto.ImageFeatureApiDto;
import shop.RecommendSystem.recommend.ImageFeature.ImageFeature;
import shop.RecommendSystem.shoppingMall.ShopService;

import java.util.*;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final SearchService searchService;
    private final ImageFeature imageFeature;
    private final ShopService shopService;

    @GetMapping("/search/findImg")
    public String findImg(
            @RequestParam(value = "backbone", defaultValue = "resnet50") String backbone,
            Model model) {
        String normalized = normalizeBackbone(backbone);
        model.addAttribute("currentBackbone", normalized);

        // 데모 페이지용 예시 상품 (썸네일 있는 최신 상품 6개)
        try {
            Map<String, Object> page = shopService.findThumbnailAll("all", 1L, 6L);
            if (page != null && page.get("items") instanceof List) {
                List<Item> demoItems = ((List<Item>) page.get("items")).stream()
                        .filter(it -> it != null && it.getImageUrl() != null)
                        .toList();
                model.addAttribute("demoItems", demoItems);
            }
        } catch (Exception e) {
            log.warn("데모 상품 조회 실패 - 빈 목록으로 진행: {}", e.getMessage());
        }
        return "search/searchResult";
    }

    private String normalizeBackbone(String backbone) {
        if (backbone == null) return "resnet50";
        String b = backbone.trim().toLowerCase();
        return ("vggnet".equals(b) || "resnet50".equals(b)) ? b : "resnet50";
    }


    /**
     * 메인 이미지 검색 (AJAX). JSON 응답:
     *  - results: List<SearchResult>
     *  - searchedImage: data URL (base64)
     *  - detections: [{className, confidence, coordinate[]}]
     *  - currentBackbone: 실제 사용된 백본명
     */
    @PostMapping("/search/img")
    @ResponseBody
    public Map<String, Object> insert(
            @RequestParam("imgFile") MultipartFile file,
            @RequestParam(value = "backbone", defaultValue = "resnet50") String backbone,
            @RequestParam(value = "useClassFilter", defaultValue = "false") boolean useClassFilter) throws Exception {

        backbone = normalizeBackbone(backbone);

        // 백본 호출 (특징점 + 객체 감지)
        ImageFeatureApiDto apiResult = imageFeature.sendImageToFastAPI(file, backbone);

        // 백본별 검색 분기
        List<SearchResult> results;
        if ("resnet50".equals(backbone)) {
            String classFilter = useClassFilter ? apiResult.getDetectedClass() : null;
            results = searchService.searchByResnet50(apiResult.getEmbedding(), classFilter, 20, null);
        } else {
            results = searchService.searchSimilarItems(apiResult.getOrder(), apiResult.getFeatures(), 20, null);
        }

        // 검색 이미지 (base64)
        String base64Image = Base64.getEncoder().encodeToString(file.getBytes());

        // 감지 데이터 수집
        List<Map<String, Object>> allDetections = new ArrayList<>();
        if (apiResult.getCoordinate() != null) {
            allDetections.add(createDetectionMap(apiResult.getDetectedClass(), apiResult.getConfidence(), apiResult.getCoordinate()));
        }
        if (apiResult.getDetections() != null) {
            for (DetectionDto d : apiResult.getDetections()) {
                if (apiResult.getCoordinate() == null || !Arrays.equals(apiResult.getCoordinate(), d.getCoordinate())) {
                    allDetections.add(createDetectionMap(d.getClassName(), d.getConfidence(), d.getCoordinate()));
                }
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("results", results);
        response.put("searchedImage", "data:" + file.getContentType() + ";base64," + base64Image);
        response.put("detections", allDetections);
        response.put("currentBackbone", backbone);
        return response;
    }

    @PostMapping("/search/img/crop")
    @ResponseBody
    public List<SearchResult> crop(
            @RequestParam("imgFile") MultipartFile file,
            @RequestParam(value = "backbone", defaultValue = "resnet50") String backbone,
            @RequestParam(value = "useClassFilter", defaultValue = "false") boolean useClassFilter) throws Exception {
        // 크롭 이미지 전용 검색 (JSON 리스트 반환). 메인 검색과 동일한 백본 파라미터 사용.
        backbone = normalizeBackbone(backbone);
        ImageFeatureApiDto apiResult = imageFeature.sendCropImageToFastAPI(file, backbone);

        if ("resnet50".equals(backbone)) {
            String classFilter = useClassFilter ? apiResult.getDetectedClass() : null;
            return searchService.searchByResnet50(apiResult.getEmbedding(), classFilter, 20, null);
        }
        return searchService.searchSimilarItems(apiResult.getOrder(), apiResult.getFeatures(), 20, null);
    }

    /**
     * ResNet-50 + PQ 검색 — A/B 비교용 별도 엔드포인트.
     *
     * 파라미터:
     *   imgFile         — 질의 이미지 (multipart)
     *   useClassFilter  — true 면 YOLO detected_class 로 사전 필터링 (기본 false)
     *   resultSize      — 최종 반환 개수 (기본 20)
     *
     * 응답: JSON (List<SearchResult>), 각 결과의 cosineSimilarity 가 정렬 기준
     */
    @PostMapping("/search/img/resnet50")
    @ResponseBody
    public List<SearchResult> searchResnet50(
            @RequestParam("imgFile") MultipartFile file,
            @RequestParam(value = "useClassFilter", defaultValue = "false") boolean useClassFilter,
            @RequestParam(value = "resultSize", defaultValue = "20") int resultSize) throws Exception {

        ImageFeatureApiDto api = imageFeature.sendImageToFastAPI(file, "resnet50");
        String classFilter = useClassFilter ? api.getDetectedClass() : null;

        log.info("[resnet50] useClassFilter={}, detectedClass={}, embedding={}bytes",
                useClassFilter, api.getDetectedClass(),
                api.getEmbedding() != null ? api.getEmbedding().length : 0);

        return searchService.searchByResnet50(api.getEmbedding(), classFilter, resultSize, null);
    }

    // 반복되는 맵 생성 로직 분리
    private Map<String, Object> createDetectionMap(String className, Object confidence, String[] coordinate) {
        return Map.of(
                "className", className,
                "confidence", confidence,
                "coordinate", coordinate
        );
    }
}
