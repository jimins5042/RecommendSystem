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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Controller
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final SearchMapper searchMapper;

    @GetMapping("/update")
    public void UpdatePhash() throws IOException {
        ArrayList<ImageInfo> list = searchMapper.findSearchUpdateTarget();

        //PHash hash = new PHash();
        ExtractByORB extractByORB = new ExtractByORB();
        HashMap<String, String> map = new HashMap<>();
        map.put("hash", "");
        map.put("uuid", "");
        AtomicInteger x = new AtomicInteger();

/*
        for (ImageInfo imageInfo : list) {

            String hash1 = extractByORB.getImageFeature(imageInfo.getImageUrl());
            map.replace("hash", hash1);
            map.replace("uuid", imageInfo.getImageUuid());
            log.info("x : {}, uuid : {} , hash : {}", x++, imageInfo.getImageUuid(), hash1);
            searchMapper.updatePHash(map);
        }
        log.info("업데이트 끝");

 */
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        List<Future<Void>> futures = new ArrayList<>();

        for (ImageInfo imageInfo : list) {
            // 각 이미지를 처리하는 작업을 스레드 풀에 제출
            futures.add(executorService.submit(() -> {
                String hash1 = extractByORB.getImageFeature(imageInfo.getImageUrl());
                //Map<String, String> map = new HashMap<>();
                map.replace("hash", hash1);
                map.replace("uuid", imageInfo.getImageUuid());
                log.info("x : {}, uuid : {} , hash : {}", x.getAndIncrement(), imageInfo.getImageUuid(), hash1);
                searchMapper.updatePHash(map);
                return null; // 반환값은 필요 없으므로 null 반환
            }));
        }

        // 모든 작업이 완료될 때까지 기다림
        for (Future<Void> future : futures) {
            try {
                future.get(); // 예외가 발생하면 예외를 던짐
            } catch (InterruptedException | ExecutionException e) {
                log.error("Error processing image: ", e);
            }
        }

        // 스레드 풀 종료
        executorService.shutdown();
    }
}
