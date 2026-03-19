package shop.RecommendSystem.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import shop.RecommendSystem.recommend.ImageFeature.VGG16;
import shop.RecommendSystem.repository.ShopRepository;
import shop.RecommendSystem.repository.mapper.ItemMapper;
import shop.RecommendSystem.repository.mapper.SearchMapper;
import shop.RecommendSystem.shoppingMall.ShopService;

@Controller
@RequiredArgsConstructor
@Slf4j
public class TestController {
    private final ShopService shopService;
    private final VGG16 vgg16;
    private final ShopRepository shopRepository;
    private final SearchMapper searchMapper;
    private final ItemMapper itemMapper;

//    @GetMapping("/update")
//    public void UpdatePhash() throws Exception {
//
//        ArrayList<PreFilterDto> list = searchMapper.speedTest();
//
//        long sum = 0L;
//        for(PreFilterDto preFilterDto : list){
//            long start = System.nanoTime();
//            searchService.searchSimilarItems(preFilterDto.getFeatureOrder(), preFilterDto.getImgFeatureValue(), 10);
//            long end = System.nanoTime() - start;
//            System.out.println("Execution Time: " + end + " ns");
//            sum += end;
//        }
//
//        System.out.println(sum / 20 + " ns");
//
//    }

// 배포할때는 잠시 주석
//    @GetMapping("/update")
//    public void UpdatePhash() throws IOException {
//        // 이미지가 저장된 폴더 경로 (예: "C:/images/")
//        //String folderPath = "C:/Users/coolc/바탕 화면/이미지 검색 자료들/gtlim/test_img";
//
//        //C:/Users/coolc/IdeaProjects/이미지 검색 자료들/gtlim/새 폴더
//        String folderPath = "C:/Users/coolc/IdeaProjects/이미지 검색 자료들/gtlim/새 폴더";
//        //String folderPath = "C:/Users/coolc/IdeaProjects/이미지 검색 자료들/gtlim";
//
//        File folder = new File(folderPath);
//
//        if (!folder.exists() || !folder.isDirectory()) {
//            log.error("지정된 경로가 존재하지 않거나 폴더가 아닙니다: {}", folderPath);
//            return;
//        }
//
//        File[] imageFiles = folder.listFiles((dir, name) -> name.toLowerCase().matches(".*\\.(jpg|jpeg|png|bmp|gif)$"));
//        if (imageFiles == null || imageFiles.length == 0) {
//            log.error("폴더에 처리할 이미지 파일이 없습니다: {}", folderPath);
//            return;
//        }
//
//
//        for (File file : imageFiles) {
//            try {
//                FileInputStream input = new FileInputStream(file);
//                MultipartFile multipartFile = new MockMultipartFile(
//                        file.getName(),
//                        file.getName(),
//                        "application/octet-stream",
//                        input
//                );
//
//                Random random = new Random();
//                long price = (random.nextInt(1000) + 1) * 100;
//
//                Item item = Item.builder()
//                        .itemTitle("Sample Item - " + file.getName())
//                        .itemContent("Description for " + file.getName())
//                        .itemPrice(price)
//                        .build();
//
//                Long id = shopService.insertItem(item, multipartFile);
//
//                item = itemMapper.findById1(id);
//
//                HashMap<String, Object> result = vgg16.sendImageToFastAPI(multipartFile);
//
//                ImageInfo imageInfo = ImageInfo.builder()
//                        .imageUuid(item.getItemImageLink())
//                        .imgFeatureValue(((byte[]) result.get("features")))
//                        .imgFeatureOrder((String) result.get("order"))
//                        .build();
//
//                int updateResult = searchMapper.updateImageInfo(imageInfo);
//
//                log.info(String.valueOf(updateResult));
//
//            } catch (IOException e) {
//                log.error("파일 처리 중 오류 발생: ", e);
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        }
//
//        log.info("업데이트 완료");
//    }



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
