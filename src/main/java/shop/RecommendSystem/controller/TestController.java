package shop.RecommendSystem.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.multipart.MultipartFile;
import shop.RecommendSystem.dto.ImageInfo;
import shop.RecommendSystem.dto.Item;
import shop.RecommendSystem.repository.mapper.ItemMapper;
import shop.RecommendSystem.shoppingMall.UploadService;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Base64;

/*
3/30
완료! 상품 6119개, 이미지 27616장 저장됨. 실패 528건

==== 재파싱 ... ===
PS C:\Users\coolc\OneDrive\Desktop> python retry_failed.py
상품 매핑 로드: 6119건
[스킵] 파싱 불가: [이미지] 선글라스/오클리_Tombstone Spoil Industrial - Safety Glass/[선글라스] 오클리_Tombstone Spoil Industrial - Safety Glass_1.jpg - src cannot be null
[스킵] 파싱 불가: [이미지] 선글라스/오클리_Tombstone Spoil Industrial - Safety Glass/[선글라스] 오클리_Tombstone Spoil Industrial - Safety Glass_4.jpg - src cannot be null
실패 이미지: 526건

완료! 복사: 526건, 스킵: 0건
CSV 저장: C:\Users\coolc\OneDrive\Desktop\marqvision\classify\csv\images_failed_retry.csv

 */

@Controller
@RequiredArgsConstructor
@Slf4j
public class TestController {
    private final UploadService uploadService;
    private final ItemMapper itemMapper;

    private static final int BATCH_SIZE = 100;

    @GetMapping("/updateItem")
    public void updateItem() throws IOException {
        String classifyPath = "C:/Users/coolc/OneDrive/Desktop/marqvision/classify";
        String csvPath = classifyPath + "/csv";

        File classifyDir = new File(classifyPath);
        if (!classifyDir.exists() || !classifyDir.isDirectory()) {
            log.error("classify 폴더가 존재하지 않습니다: {}", classifyPath);
            return;
        }

        new File(csvPath).mkdirs();

        File[] categoryDirs = classifyDir.listFiles(File::isDirectory);
        if (categoryDirs == null || categoryDirs.length == 0) {
            log.error("카테고리 폴더가 없습니다: {}", classifyPath);
            return;
        }

        int threadCount = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        log.info("스레드 풀 크기: {}", threadCount);

        Random random = new Random();
        int totalItems = 0;
        int totalImages = 0;
        int batchNumber = 1;
        long itemIdSeq = 1;
        AtomicLong imageIdSeq = new AtomicLong(1);

        List<Item> itemBatch = new ArrayList<>();
        List<Future<ImageInfo>> imageFutures = new ArrayList<>();
        List<String> imageLabels = new ArrayList<>();
        List<String> failedList = new ArrayList<>();

        for (File categoryDir : categoryDirs) {
            String category = categoryDir.getName();
            if ("csv".equals(category)) continue;

            File[] productDirs = categoryDir.listFiles(File::isDirectory);
            if (productDirs == null) continue;

            for (File productDir : productDirs) {
                String productName = productDir.getName();

                File[] imageFiles = productDir.listFiles((dir, name) ->
                        name.toLowerCase().matches(".*\\.(jpg|jpeg|png|bmp|gif|webp)$"));
                if (imageFiles == null || imageFiles.length == 0) continue;

                long itemId = itemIdSeq++;
                long price = (random.nextInt(1000) + 1) * 100;

                Item item = Item.builder()
                        .itemId(itemId)
                        .itemTitle(productName)
                        .itemContent("Description for " + productName)
                        .itemPrice(price)
                        .category(category)
                        .build();
                itemBatch.add(item);

                // 이미지 처리 — 스레드풀에 제출
                for (File imgFile : imageFiles) {
                    long imageId = imageIdSeq.getAndIncrement();
                    imageLabels.add(category + "/" + productName + "/" + imgFile.getName());

                    imageFutures.add(executor.submit(() -> {
                        try (FileInputStream input = new FileInputStream(imgFile)) {
                            MultipartFile mf = new MockMultipartFile(
                                    imgFile.getName(), imgFile.getName(),
                                    "application/octet-stream", input);
                            return uploadService.processFile(mf, imageId, itemId, category);
                        }
                    }));
                }

                totalItems++;

                // 배치 크기 도달 시 — Future 수거 후 CSV 기록
                if (itemBatch.size() >= BATCH_SIZE) {
                    List<ImageInfo> imageBatch = collectResults(imageFutures, imageLabels, failedList);
                    totalImages += imageBatch.size();
                    writeBatch(csvPath, batchNumber++, itemBatch, imageBatch);
                    log.info("[배치 #{}] 상품 {}건, 이미지 {}건", batchNumber - 1, itemBatch.size(), imageBatch.size());
                    itemBatch.clear();
                    imageFutures.clear();
                    imageLabels.clear();
                }
            }
        }

        // 남은 배치 처리
        if (!itemBatch.isEmpty()) {
            List<ImageInfo> imageBatch = collectResults(imageFutures, imageLabels, failedList);
            totalImages += imageBatch.size();
            writeBatch(csvPath, batchNumber, itemBatch, imageBatch);
            log.info("[배치 #{}] 상품 {}건, 이미지 {}건", batchNumber, itemBatch.size(), imageBatch.size());
        }

        executor.shutdown();

        // 실패 목록 기록
        if (!failedList.isEmpty()) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(csvPath + "/failed.log"))) {
                failedList.forEach(pw::println);
            }
            log.warn("===== 실패 목록 ({}건) =====", failedList.size());
            failedList.forEach(log::warn);
        }

        log.info("완료! 상품 {}개, 이미지 {}장 저장됨. 실패 {}건", totalItems, totalImages, failedList.size());
    }

    private List<ImageInfo> collectResults(List<Future<ImageInfo>> futures, List<String> labels, List<String> failedList) {
        List<ImageInfo> results = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            try {
                ImageInfo info = futures.get(i).get();
                results.add(info);
            } catch (Exception e) {
                failedList.add("[이미지] " + labels.get(i) + " - " + e.getCause().getMessage());
            }
        }
        return results;
    }

    private void writeBatch(String csvPath, int batchNumber, List<Item> items, List<ImageInfo> images) throws IOException {
        String suffix = String.format("_%03d.csv", batchNumber);

        // items CSV
        try (PrintWriter pw = new PrintWriter(new FileWriter(csvPath + "/items" + suffix))) {
            pw.println("item_id,item_title,item_content,item_price,category");
            for (Item item : items) {
                pw.printf("%d,\"%s\",\"%s\",%d,\"%s\"%n",
                        item.getItemId(),
                        escapeCsv(item.getItemTitle()),
                        escapeCsv(item.getItemContent()),
                        item.getItemPrice(),
                        escapeCsv(item.getCategory()));
            }
        }

        // images CSV
        try (PrintWriter pw = new PrintWriter(new FileWriter(csvPath + "/images" + suffix))) {
            pw.println("item_id,image_uuid,image_original_name,image_url,image_hash_code");
            for (ImageInfo img : images) {
                pw.printf("%d,\"%s\",\"%s\",\"%s\",\"%s\"%n",
                        img.getItemId(),
                        img.getImageUuid(),
                        escapeCsv(img.getImageOriginalName()),
                        img.getImageUrl(),
                        img.getImageHashCode());
            }
        }

        log.info("[CSV] 배치 #{} 저장 - 상품 {}건, 이미지 {}건", batchNumber, items.size(), images.size());
    }

    @GetMapping("/importCsv")
    public void importCsv() throws IOException {
        String csvPath = "C:/Users/coolc/OneDrive/Desktop/marqvision/classify/csv";
        File csvDir = new File(csvPath);

        File[] itemsCsvFiles = csvDir.listFiles((dir, name) ->
                name.startsWith("images_failed_retry") && name.endsWith(".csv"));
        if (itemsCsvFiles == null || itemsCsvFiles.length == 0) {
            log.error("CSV 파일이 없습니다: {}", csvPath);
            return;
        }

        Arrays.sort(itemsCsvFiles);

        int totalItems = 0;
        int totalImages = 0;


        for (File itemsCsv : itemsCsvFiles) {

            try{
                Thread.sleep(1000);
            }catch (InterruptedException e){

            }


            // items_001.csv → images_001.csv
            String number = itemsCsv.getName().replace("items_", "").replace(".csv", "");
            File imagesCsv = new File(csvPath, "images_" + number + ".csv");

            // 1) 상품 CSV 읽기 + DB 시퀀스 매핑
            //Map<Long, Long> idMapping = new HashMap<>(); // csvItemId → dbItemId
            List<Item> items = new ArrayList<>();

            try (BufferedReader br = new BufferedReader(new FileReader(itemsCsv))) {
                br.readLine(); // 헤더 스킵
                String line;
                while ((line = br.readLine()) != null) {
                    String[] fields = parseCsvLine(line);
                    long csvItemId = Long.parseLong(fields[0]);
                    //long dbItemId = itemMapper.getShopBoardSeq();
                    //idMapping.put(csvItemId, dbItemId);

                    items.add(Item.builder()
                            .itemId(Long.valueOf(fields[0]))
                            .itemTitle(fields[1])
                            .itemContent(fields[2])
                            .itemPrice(Long.parseLong(fields[3]))
                            .category(fields[4])
                            .build());
                }
            }

            if (!items.isEmpty()) {
                itemMapper.bulkInsert(items);
                totalItems += items.size();
            }

            // 2) 이미지 CSV 읽기 + itemId 매핑 치환
            if (imagesCsv.exists()) {
                List<ImageInfo> images = new ArrayList<>();

                try (BufferedReader br = new BufferedReader(new FileReader(imagesCsv))) {
                    br.readLine(); // 헤더 스킵
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] fields = parseCsvLine(line);
                        long csvItemId = Long.parseLong(fields[0]);
                        //long dbItemId = idMapping.getOrDefault(csvItemId, csvItemId);

                        images.add(ImageInfo.builder()
                                .itemId(Long.valueOf(fields[0]))
                                .imageUuid(fields[1])
                                .imageOriginalName(fields[2])
                                .imageUrl(fields[3])
                                .imageHashCode(fields[4])
                                .build());
                    }
                }

                if (!images.isEmpty()) {
                    itemMapper.insertBulkImageInfo(images);
                    totalImages += images.size();
                }
            }

            log.info("[임포트] {} - 상품 {}건, 이미지 {}건",
                    itemsCsv.getName(), items.size(),
                    imagesCsv.exists() ? totalImages : 0);
        }

        log.info("CSV 임포트 완료! 상품 {}개, 이미지 {}장", totalItems, totalImages);
    }

    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        sb.append('"');
                        i++; // "" → "
                    } else {
                        inQuotes = false;
                    }
                } else {
                    sb.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(sb.toString());
                    sb.setLength(0);
                } else {
                    sb.append(c);
                }
            }
        }
        fields.add(sb.toString());
        return fields.toArray(new String[0]);
    }

    /**
     * features.csv를 읽어서 image_info 테이블에 bulk update
     * 흐름: 스테이징 테이블 생성 → bulk insert → MERGE → 스테이징 DROP
     */
    @GetMapping("/importFeatures")
    public void importFeatures() throws IOException {
        String csvDirPath = "C:/Users/coolc/OneDrive/Desktop/marqvision/classify/csv";
        File csvDir = new File(csvDirPath);

        File[] featureFiles = csvDir.listFiles((dir, name) ->
                name.startsWith("features_") && name.endsWith(".csv"));

        if (featureFiles == null || featureFiles.length == 0) {
            log.error("features_*.csv 파일이 없습니다: {}", csvDirPath);
            return;
        }

        Arrays.sort(featureFiles);

        // 1) 스테이징 테이블 생성
        try {
            itemMapper.dropFeatureStaging();
        } catch (Exception ignored) {
            // 테이블이 없으면 무시
        }
        itemMapper.createFeatureStaging();
        log.info("[Feature Import] 스테이징 테이블 생성 완료");

        // 2) 모든 파일 읽어서 스테이징 INSERT
        int totalRows = 0;
        int batchNum = 0;
        List<ImageInfo> batch = new ArrayList<>();

        for (File csvFile : featureFiles) {
            log.info("[Feature Import] 파일 처리 중: {}", csvFile.getName());
            try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
                br.readLine(); // 헤더 스킵
                String line;
                while ((line = br.readLine()) != null) {
                    String[] fields = parseCsvLine(line);
                    if (fields.length < 3) continue;

                    String image_uuid = fields[0];
                    byte[] featureValue = Base64.getDecoder().decode(fields[1]);
                    String featureOrder = fields[2];

                    batch.add(ImageInfo.builder()
                            .imageUuid(image_uuid) // 현재 1열에 저장된 uuid 값 사용
                            .imgFeatureValue(featureValue)
                            .imgFeatureOrder(featureOrder)
                            .build());

                    if (batch.size() >= BATCH_SIZE) {
                        itemMapper.insertFeatureStaging(batch);
                        totalRows += batch.size();
                        batchNum++;
                        log.info("[Feature Import] 스테이징 INSERT 배치 #{} - {}건 (누적 {}건)",
                                batchNum, batch.size(), totalRows);
                        batch.clear();
                    }
                }
            }
        }

        // 남은 배치
        if (!batch.isEmpty()) {
            itemMapper.insertFeatureStaging(batch);
            totalRows += batch.size();
            batchNum++;
            log.info("[Feature Import] 스테이징 INSERT 배치 #{} - {}건 (누적 {}건)",
                    batchNum, batch.size(), totalRows);
            batch.clear();
        }

        // 3) MERGE
        log.info("[Feature Import] MERGE 실행 중... ({}건)", totalRows);
        itemMapper.mergeFeatureFromStaging();
        log.info("[Feature Import] MERGE 완료");

        // 4) 스테이징 테이블 삭제
        //itemMapper.dropFeatureStaging();
        log.info("[Feature Import] 스테이징 테이블 삭제 완료. 총 {}건 업데이트", totalRows);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
}
