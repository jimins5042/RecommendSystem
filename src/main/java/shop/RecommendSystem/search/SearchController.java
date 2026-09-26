package shop.RecommendSystem.search;


import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import shop.RecommendSystem.dto.*;
import shop.RecommendSystem.recommend.ImageFeature.ImageFeature;
import shop.RecommendSystem.recommend.ItemFiltering.PQFiltering;
import shop.RecommendSystem.recommend.ItemFiltering.SparseFeatureIndexing;
import shop.RecommendSystem.repository.mapper.ItemMapper;

import java.util.*;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SearchController {


    private final SearchService searchService;
    private final ImageFeature imageFeature;
    private final ItemMapper itemMapper;

    @GetMapping("/search/findImg")
    public String findImg(HttpSession session,
                          @RequestParam(value = "backbone", defaultValue = "resnet50") String backbone,
                          Model model) {

        backbone = normalizeBackbone(backbone);

        model.addAttribute("currentBackbone", backbone);

        // 데모 페이지용 예시 상품 (썸네일 있는 최신 상품 6개)
        try {
            List<Item> demoItems = itemMapper.selectRandomList(2L, session.getId());
            model.addAttribute("demoItems", demoItems);

        } catch (
                Exception e) {
            log.warn("데모 상품 조회 실패 - 빈 목록으로 진행: {}", e.getMessage());
        }
        return "search/searchResult";
    }

    private String normalizeBackbone(String backbone) {

        if(backbone == null || (!"resnet50".equals(backbone) && !("vggnet").equals(backbone))) {
            backbone = "resnet50";
        }

        return backbone;
    }


    /**
     * 메인 이미지 검색 (AJAX). JSON 응답:
     * - results: List<SearchResult>
     * - searchedImage: data URL (base64)
     * - detections: [{className, confidence, coordinate[]}]
     * - currentBackbone: 실제 사용된 백본명
     */
    @PostMapping("/search/img")
    @ResponseBody
    public Map<String, Object> insert(
            @RequestParam("imgFile") MultipartFile file,
            @RequestParam(value = "backbone", defaultValue = "resnet50") String backbone) throws Exception {

        backbone = normalizeBackbone(backbone);

        // 백본 호출 (특징점 + 객체 감지)
        ImageFeatureApiDto apiResult = imageFeature.sendImageToFastAPI(file, backbone);

        // 백본별 검색 분기
        List<SearchResult> results = searchService.searchBranchByBackbone(backbone, apiResult);


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
            @RequestParam(value = "backbone", defaultValue = "resnet50") String backbone) throws Exception {

        // 크롭 이미지 전용 검색 (JSON 리스트 반환). 메인 검색과 동일한 백본 파라미터 사용.
        backbone = normalizeBackbone(backbone);
        ImageFeatureApiDto apiResult = imageFeature.sendCropImageToFastAPI(file, backbone);

        // 백본별 검색 분기
        List<SearchResult> results = searchService.searchBranchByBackbone(backbone, apiResult);

        return results;
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
