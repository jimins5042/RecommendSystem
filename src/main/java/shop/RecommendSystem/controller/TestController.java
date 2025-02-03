package shop.RecommendSystem.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import shop.RecommendSystem.dto.ImageInfo;
import shop.RecommendSystem.recommend.ImageFeature.ExtractByORB;
import shop.RecommendSystem.repository.mapper.SearchMapper;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.FileInputStream;
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

    @GetMapping("/update1")
    public void UpdatePhash1() throws IOException {
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

    @GetMapping("/update")
    public void UpdatePhash() throws IOException {
        // 이미지가 저장된 폴더 경로 (예: "C:/images/")
        String folderPath = "C:/Users/coolc/바탕 화면/이미지 검색 자료들/gtlim/test_img";
        File folder = new File(folderPath);

        if (!folder.exists() || !folder.isDirectory()) {
            log.error("지정된 경로가 존재하지 않거나 폴더가 아닙니다: {}", folderPath);
            return;
        }

        File[] imageFiles = folder.listFiles((dir, name) -> name.toLowerCase().matches(".*\\.(jpg|jpeg|png|bmp|gif)$"));
        if (imageFiles == null || imageFiles.length == 0) {
            log.error("폴더에 처리할 이미지 파일이 없습니다: {}", folderPath);
            return;
        }

        ExtractByORB extractByORB = new ExtractByORB();
        AtomicInteger x = new AtomicInteger();
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        List<Future<Void>> futures = new ArrayList<>();

        for (File file : imageFiles) {
            futures.add(executorService.submit(() -> {
                try {
                    FileInputStream input = new FileInputStream(file);
                    MultipartFile multipartFile = new MockMultipartFile(
                            file.getName(), file.getName(), "image/jpeg", input);

                    // 해시 값 추출
                    String hash1 = extractByORB.getImageFeature(multipartFile);

                    // DB 업데이트 (uuid는 파일명 기반으로 예제 처리)
                    HashMap<String, String> map = new HashMap<>();
                    map.put("hash", hash1);
                    map.put("uuid", file.getName());

                    log.info("x : {}, 파일명 : {} , hash : {}", x.getAndIncrement(), file.getName(), hash1);
                    searchMapper.updatePHash(map);
                } catch (IOException e) {
                    log.error("파일 처리 중 오류 발생: ", e);
                }
                return null;
            }));
        }

        // 모든 작업이 완료될 때까지 대기
        for (Future<Void> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                log.error("이미지 처리 중 오류 발생: ", e);
            }
        }

        executorService.shutdown();
        log.info("업데이트 완료");
    }
}
