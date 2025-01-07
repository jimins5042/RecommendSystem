package shop.RecommendSystem.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import shop.RecommendSystem.service.logic.ImageProcessing;
import shop.RecommendSystem.service.logic.LSHService;
import shop.RecommendSystem.service.logic.PHash;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/search")
public class SearchController {

    private final ImageProcessing imgCtrl;
    private final LSHService lshService;

    @GetMapping("/findImg")
    public String findImg(Model model) {
        return "/search/TestSearchForm";
    }

    @PostMapping("/findImg")
    @ResponseBody
    public Map<String, String> findImg(@RequestParam("image") MultipartFile file,
                                       @RequestParam("rgb") String palette) throws IOException {
        //이미지 특징 추출 후 저장
        Map<String, String> response = new HashMap<>();

        if (!file.isEmpty()) {

            long beforeTime = System.currentTimeMillis();
            PHash pHash = new PHash();
            String hashValue = pHash.getPHash(file);
            log.info("hashValue: " + hashValue);
            response.put("hashValue", hashValue);
            response.put("runTime", String.valueOf(System.currentTimeMillis() - beforeTime));

            HashMap<String, Double> map= lshService.searchLSH(hashValue);
            log.info("map: " + map.size());

        }
        ObjectMapper objectMapper = new ObjectMapper();
        int[] paletteArray = objectMapper.readValue(palette, int[].class);
        String nearestColor = imgCtrl.getNearestColor(paletteArray);

        log.info("nearestColor: " + nearestColor);



        response.put("nearestColor", nearestColor);


        return response;

    }




}
