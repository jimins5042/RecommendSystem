package shop.RecommendSystem.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import shop.RecommendSystem.dto.ImageInfo;
import shop.RecommendSystem.repository.mapper.SearchMapper;
import shop.RecommendSystem.service.logic.ImageFeature;
import shop.RecommendSystem.service.logic.PHash;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

@Controller
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final SearchMapper searchMapper;

    //@GetMapping("/update")
    public void UpdatePhash() throws IOException {
        ArrayList<ImageInfo> list = searchMapper.findUpdateTarget();

        //PHash hash = new PHash();
        ImageFeature imageFeature = new ImageFeature();
        HashMap<String, String> map = new HashMap<>();
        map.put("hash", "");
        map.put("uuid", "");
        int x = 0;
        for (ImageInfo imageInfo : list) {
            String hash1 = imageFeature.getImageFeature(imageInfo.getImageUrl()).toString();
            //String hash1 = hash.getPHash(imageInfo.getImageUrl());
            map.replace("hash", hash1);
            map.replace("uuid", imageInfo.getImageUuid());
            log.info("x : {}, uuid : {} , hash : {}", x++, imageInfo.getImageUuid(), hash1);
            searchMapper.updatePHash(map);
        }
        log.info("업데이트 끝");
    }
}
