package shop.RecommendSystem;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import shop.RecommendSystem.dto.ImageFeatureApiDto;
import shop.RecommendSystem.dto.SearchResult;
import shop.RecommendSystem.recommend.ImageFeature.ImageFeature;
import shop.RecommendSystem.search.SearchService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@SpringBootTest
class RecommendSystemApplicationTests {

    @Autowired
    private SearchService searchService;

    @Autowired
    private ImageFeature imageFeature;

    private static final String TEST_QUERIES_PATH =
            "C:/Users/coolc/PycharmProjects/recommandSystem-py/ground_truth_data/test_queries.json";

    private static final String IMAGE_ROOT =
            //"C:/Users/coolc/PycharmProjects/recommandSystem-py/upload";
    "C:/Users/coolc/IdeaProjects/RecommendSystem/src/main/resources/upload";

    private static final String RESULT_PATH =
            "C:/Users/coolc/PycharmProjects/recommandSystem-py/ground_truth_data/eval_result.json";

    // ── 내부 DTO ────────────────────────────────────────────────────────────

    record TestQuery(
            String queryId,
            String imageUuid,
            String imageUrl,
            int groundTruthCount,
            List<String> groundTruth
    ) {}

    record EvalResult(
            String queryId,
            int retrieved,
            int relevant,
            int hit,
            double precision,
            double recall
    ) {}

    // ── 테스트 진입점 ────────────────────────────────────────────────────────

    @Test
    void contextLoads() {}

    @Test
    void evaluateSearchAccuracy() throws Exception {
        Map<String, List<TestQuery>> testQueries = loadTestQueries();
        Map<String, List<EvalResult>> allResults = new LinkedHashMap<>();

        for (var entry : testQueries.entrySet()) {
            String category = entry.getKey();
            List<EvalResult> catResults = new ArrayList<>();

            System.out.printf("%n[%s] 쿼리 %d개 평가 시작%n", category, entry.getValue().size());

            for (TestQuery query : entry.getValue()) {
                MockMultipartFile image = loadImageAsMultipart(query.imageUrl());
                if (image == null) {
                    System.out.printf("  [경고] 이미지 로드 실패, 스킵: %s%n", query.queryId());
                    continue;
                }

                //List<String> predicted = callSearchMethod(image);
                // VGG16 API 호출 (특징점 + 객체 감지)
                ImageFeatureApiDto apiResult = imageFeature.sendImageToFastAPI(image, "efficientnet");

                // 특징점으로 유사 상품 검색
                List<SearchResult> results = searchService.searchSimilarItems(apiResult.getOrder(), apiResult.getFeatures(), 10, null);
                List<String> predicted = new ArrayList<>();

                for(SearchResult result : results) {
                    predicted.add(result.getImageUuid());
                }

                System.out.println("  [DEBUG] predicted   : " + predicted);
                System.out.println("  [DEBUG] groundTruth : " + query.groundTruth().subList(0, Math.min(5, query.groundTruth().size())));

                EvalResult result = evaluate(query.queryId(), predicted, query.groundTruth());
                catResults.add(result);

                System.out.printf("  %s | hit=%d/%d | P=%.3f R=%.3f%n",
                        result.queryId(), result.hit(), result.relevant(),
                        result.precision(), result.recall());
            }

            allResults.put(category, catResults);
        }

        saveResults(allResults);
        printSummary(allResults);
    }

    // ── 사용자가 구현할 stub ─────────────────────────────────────────────────

    private List<String> callSearchMethod(MockMultipartFile image) throws Exception {
        ImageFeatureApiDto apiResult = imageFeature.sendImageToFastAPI(image, "efficientnet");
        List<SearchResult> results = searchService.searchSimilarItems(
                apiResult.getOrder(), apiResult.getFeatures(), 10, null);
        return results.stream().map(SearchResult::getImageUuid).toList();
    }

    // ── JSON 파싱 ────────────────────────────────────────────────────────────

    private Map<String, List<TestQuery>> loadTestQueries() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        Map<String, List<Map<String, Object>>> raw = mapper.readValue(
                Paths.get(TEST_QUERIES_PATH).toFile(),
                new TypeReference<>() {}
        );

        Map<String, List<TestQuery>> result = new LinkedHashMap<>();
        for (var entry : raw.entrySet()) {
            List<TestQuery> queries = entry.getValue().stream()
                    .map(m -> new TestQuery(
                            (String) m.get("query_id"),
                            (String) m.get("image_uuid"),
                            (String) m.get("image_url"),
                            (int) m.get("ground_truth_count"),
                            (List<String>) m.get("ground_truth")
                    ))
                    .toList();
            result.put(entry.getKey(), queries);
        }

        int total = result.values().stream().mapToInt(List::size).sum();
        System.out.printf("test_queries.json 로드 완료: %d개 카테고리, %d개 쿼리%n",
                result.size(), total);
        return result;
    }

    // ── 이미지 로드 ──────────────────────────────────────────────────────────

    private MockMultipartFile loadImageAsMultipart(String imageUrl) {
        // /upload/bag/xxx.jpg → IMAGE_ROOT/bag/xxx.jpg
        String relativePath = imageUrl.replace("/upload/", "");
        Path filePath = Paths.get(IMAGE_ROOT, relativePath);
        System.out.println("==== filePath : " +  filePath.toString());
        if (!Files.exists(filePath)) {
            return null;
        }

        try {
            byte[] bytes = Files.readAllBytes(filePath);
            String filename = filePath.getFileName().toString();
            return new MockMultipartFile("image", filename, "image/jpeg", bytes);
        } catch (IOException e) {
            System.out.printf("  [오류] 이미지 읽기 실패: %s — %s%n", filePath, e.getMessage());
            return null;
        }
    }

    // ── 평가 지표 계산 ───────────────────────────────────────────────────────

    private EvalResult evaluate(String queryId, List<String> predicted, List<String> groundTruth) {
        Set<String> gtSet = new HashSet<>(groundTruth);
        int hit = (int) predicted.stream().filter(gtSet::contains).count();
        int retrieved = predicted.size();
        int relevant = groundTruth.size();

        double precision = retrieved > 0 ? (double) hit / retrieved : 0.0;
        double recall    = relevant  > 0 ? (double) hit / relevant  : 0.0;

        return new EvalResult(queryId, retrieved, relevant, hit, precision, recall);
    }

    // ── 결과 저장 ────────────────────────────────────────────────────────────

    private void saveResults(Map<String, List<EvalResult>> allResults) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> summary = new LinkedHashMap<>();
        for (var entry : allResults.entrySet()) {
            List<EvalResult> results = entry.getValue();
            if (results.isEmpty()) continue;

            double avgPrecision = results.stream().mapToDouble(EvalResult::precision).average().orElse(0);
            double avgRecall    = results.stream().mapToDouble(EvalResult::recall).average().orElse(0);

            summary.put(entry.getKey(), Map.of(
                    "avg_precision", Math.round(avgPrecision * 1000.0) / 1000.0,
                    "avg_recall",    Math.round(avgRecall    * 1000.0) / 1000.0,
                    "query_count",   results.size()
            ));
        }

        Map<String, Object> details = new LinkedHashMap<>();
        for (var entry : allResults.entrySet()) {
            details.put(entry.getKey(), entry.getValue().stream()
                    .map(r -> Map.of(
                            "query_id",  r.queryId(),
                            "retrieved", r.retrieved(),
                            "relevant",  r.relevant(),
                            "hit",       r.hit(),
                            "precision", Math.round(r.precision() * 1000.0) / 1000.0,
                            "recall",    Math.round(r.recall()    * 1000.0) / 1000.0
                    ))
                    .toList());
        }

        Map<String, Object> output = Map.of("summary", summary, "details", details);
        mapper.writerWithDefaultPrettyPrinter()
              .writeValue(Paths.get(RESULT_PATH).toFile(), output);

        System.out.printf("%n결과 저장 완료: %s%n", RESULT_PATH);
    }

    // ── 콘솔 요약 출력 ───────────────────────────────────────────────────────

    private void printSummary(Map<String, List<EvalResult>> allResults) {
        System.out.println("\n========== 평가 결과 요약 ==========");
        for (var entry : allResults.entrySet()) {
            List<EvalResult> results = entry.getValue();
            if (results.isEmpty()) continue;
            double avgP = results.stream().mapToDouble(EvalResult::precision).average().orElse(0);
            double avgR = results.stream().mapToDouble(EvalResult::recall).average().orElse(0);
            System.out.printf("  %-15s | Precision=%.3f | Recall=%.3f | 쿼리수=%d%n",
                    entry.getKey(), avgP, avgR, results.size());
        }
        System.out.println("====================================");
    }
}
