package shop.RecommendSystem.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import shop.RecommendSystem.service.ImageControlLogicService;
import shop.RecommendSystem.service.ImagePHash;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/search")
public class SearchController {

    private final ImageControlLogicService imgCtrl;

    @GetMapping("/findImg")
    public String findImg(Model model) {
        return "/search/TestSearchForm";
    }

    @PostMapping("/findImg")
    @ResponseBody
    public Map<String, String> findImg(@RequestParam("image") MultipartFile file,
                                       @RequestParam("rgb") String  palette) throws IOException {
        //이미지 특징 추출 후 저장
        Map<String, String> response = new HashMap<>();

        if (!file.isEmpty()) {

            long beforeTime = System.currentTimeMillis();
            ImagePHash pHash = new ImagePHash();
            String hashValue = pHash.getPHash(file);
            log.info("hashValue: " + hashValue);
            response.put("hashValue", hashValue);


            response.put("runTime", String.valueOf(System.currentTimeMillis() - beforeTime));
        }
        ObjectMapper objectMapper = new ObjectMapper();
        int[] paletteArray = objectMapper.readValue(palette, int[].class);
        String nearestColor = imgCtrl.getNearestColor(paletteArray);

        log.info("nearestColor: " + nearestColor);

        response.put("nearestColor", nearestColor);


        return response;

    }


}
