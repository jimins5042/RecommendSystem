package shop.RecommendSystem.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import shop.RecommendSystem.dto.Item;
import shop.RecommendSystem.dto.PreFilterDto;
import shop.RecommendSystem.repository.ShopRepository;
import shop.RecommendSystem.repository.mapper.SearchMapper;
import shop.RecommendSystem.search.SearchService;
import shop.RecommendSystem.shoppingMall.ShopService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class TestController {
    private final ShopRepository shopRepository;
    private final ShopService shopService;
    private final SearchMapper searchMapper;
private final SearchService searchService;

    @GetMapping("/update")
    public void UpdatePhash() throws Exception {

        ArrayList<PreFilterDto> list = searchMapper.speedTest();

        long sum = 0L;
        for(PreFilterDto preFilterDto : list){
            long start = System.nanoTime();
            searchService.searchSimilarItems(preFilterDto.getFeatureOrder(), preFilterDto.getImgFeatureValue(), 10);
            long end = System.nanoTime() - start;
            System.out.println("Execution Time: " + end + " ns");
            sum += end;
        }

        System.out.println(sum / 20 + " ns");

    }


    /*
    @GetMapping("/update")
    public void UpdatePhash() throws IOException {
        // 이미지가 저장된 폴더 경로 (예: "C:/images/")
        //String folderPath = "C:/Users/coolc/바탕 화면/이미지 검색 자료들/gtlim/test_img";
        String folderPath = "C:/Users/coolc/바탕 화면/새 폴더";
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

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        List<Future<Void>> futures = new ArrayList<>();

        for (File file : imageFiles) {
            futures.add(executorService.submit(() -> {
                try {
                    FileInputStream input = new FileInputStream(file);
                    MultipartFile multipartFile = new MockMultipartFile(
                            file.getName(), file.getName(), "image/jpeg", input);

                    shopService.insertItem(
                            new Item("Sample Item - " + file.getName(), "Description for " + file.getName(), 10000L, ""),
                            multipartFile, "");

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

     */


/*
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

 */
}
