package shop.RecommendSystem.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import shop.RecommendSystem.dto.ImageInfo;
import shop.RecommendSystem.recommend.ImageFeature.ExtractByORB;
import shop.RecommendSystem.repository.mapper.SearchMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final SearchMapper searchMapper;

    //@GetMapping("/update")
    public void UpdatePhash() throws IOException {
        ArrayList<ImageInfo> list = searchMapper.findSearchUpdateTarget();

        //PHash hash = new PHash();
        ExtractByORB extractByORB = new ExtractByORB();
        HashMap<String, String> map = new HashMap<>();
        map.put("hash", "");
        map.put("uuid", "");
        int x = 0;
        for (ImageInfo imageInfo : list) {
            List<Double> list1 = extractByORB.getImageFeature(imageInfo.getImageUrl());

            String hash1 = extractByORB.encodeFeaturesAsHex(list1);
            //String hash1 = hash.getPHash(imageInfo.getImageUrl());
            map.replace("hash", hash1);
            map.replace("uuid", imageInfo.getImageUuid());
            log.info("x : {}, uuid : {} , hash : {}", x++, imageInfo.getImageUuid(), hash1);
            searchMapper.updatePHash(map);
        }
        log.info("업데이트 끝");
    }
}
