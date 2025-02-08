package shop.RecommendSystem.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import shop.RecommendSystem.dto.SearchResult;
import shop.RecommendSystem.recommend.ImageFeature.PHash;
import shop.RecommendSystem.service.ImageProcessing;
import shop.RecommendSystem.service.SearchService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/search")
public class SearchController {

    private final ImageProcessing imgCtrl;
    private final SearchService searchService;


    @GetMapping("/findImg")
    public String findImg(Model model) {
        return "search/TestSearchForm";
    }

    @PostMapping("/findImg")
    @ResponseBody
    public Map<String, Object> findImg(@RequestParam("image") MultipartFile file,
                                       @RequestParam("rgb") String palette) throws IOException {
        //이미지 특징 추출 후 저장
        Map<String, Object> response = new HashMap<>();

        if (!file.isEmpty()) {

            long beforeTime = System.currentTimeMillis();

            String hashValue = new PHash().getPHash(file);
            log.info("hashValue: " + hashValue);

            List<SearchResult> results = searchService.searchSimilarItems(hashValue, 10);

            response.put("hashValue", hashValue);
            response.put("runTime", String.valueOf(System.currentTimeMillis() - beforeTime));
            response.put("items", results);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        int[] paletteArray = objectMapper.readValue(palette, int[].class);
        String nearestColor = imgCtrl.getNearestColor(paletteArray);

        log.info("nearestColor: " + nearestColor);

        response.put("nearestColor", nearestColor);

        return response;

    }

    @PostMapping("/img")
    public String insert(
            @RequestParam("query") String query,
            @RequestParam("imgFile") MultipartFile file, Model model) throws IOException {

        log.info("query: " + query);

        List<SearchResult> results = searchService.searchSimilarItems(new PHash().getPHash(file), 10);
        model.addAttribute("results", results);

        return "shop/itemMain";  // 템플릿 파일 경로
    }


}
