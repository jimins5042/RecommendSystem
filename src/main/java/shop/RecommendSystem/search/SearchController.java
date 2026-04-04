package shop.RecommendSystem.search;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import shop.RecommendSystem.dto.DetectionDto;
import shop.RecommendSystem.dto.SearchResult;
import shop.RecommendSystem.dto.VGG16ApiDto;
import shop.RecommendSystem.recommend.ImageFeature.VGG16;
import shop.RecommendSystem.recommend.ImageProcessing;

import java.util.*;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final ImageProcessing imgCtrl;
    private final SearchService searchService;
    private final VGG16 vgg16;

    @GetMapping("/search/findImg")
    public String findImg(Model model) {
        return "search/searchResult";
    }


    @PostMapping("/search/img")
    public String insert(
            @RequestParam("query") String query,
            @RequestParam("imgFile") MultipartFile file, Model model) throws Exception {

        // VGG16 API 호출 (특징점 + 객체 감지)
        VGG16ApiDto apiResult = vgg16.sendImageToFastAPI(file);

        // 특징점으로 유사 상품 검색
        List<SearchResult> results = searchService.searchSimilarItems(apiResult.getOrder(), apiResult.getFeatures(), 10, null);

        // 검색 이미지 (base64)
        String base64Image = Base64.getEncoder().encodeToString(file.getBytes());

        // 감지 데이터 수집 (스트림/리스트 활용으로 간소화)
        List<Map<String, Object>> allDetections = new ArrayList<>();

        // 메인 객체 추가
        if (apiResult.getCoordinate() != null) {
            allDetections.add(createDetectionMap(apiResult.getDetectedClass(), apiResult.getConfidence(), apiResult.getCoordinate()));
        }

        // 추가 객체들 중복 제외 추가
        if (apiResult.getDetections() != null) {
            for (DetectionDto d : apiResult.getDetections()) {
                if (apiResult.getCoordinate() == null || !Arrays.equals(apiResult.getCoordinate(), d.getCoordinate())) {
                    allDetections.add(createDetectionMap(d.getClassName(), d.getConfidence(), d.getCoordinate()));
                }
            }
        }

        ObjectMapper mapper = new ObjectMapper();
        model.addAttribute("results", results);
        model.addAttribute("searchedImage", "data:" + file.getContentType() + ";base64," + base64Image);
        model.addAttribute("detectionsJson", mapper.writeValueAsString(allDetections));

        return "search/searchResult";
    }

    @PostMapping("/search/img/crop")
    @ResponseBody
    public List<SearchResult> crop(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam("imgFile") MultipartFile file) throws Exception {
        
        // 크롭 이미지 전용 검색 (JSON 리스트 반환)
        VGG16ApiDto apiResult = vgg16.sendCropImageToFastAPI(file);
        return searchService.searchSimilarItems(apiResult.getOrder(), apiResult.getFeatures(), 10, null);
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
