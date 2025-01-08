package shop.RecommendSystem.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import shop.RecommendSystem.dto.SearchResult;
import shop.RecommendSystem.repository.mapper.SearchMapper;
import shop.RecommendSystem.service.logic.ImageProcessing;
import shop.RecommendSystem.service.logic.LSHService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final ImageProcessing imgCtrl;
    private final LSHService lshService;
    private final SearchMapper searchMapper;

    public List<SearchResult> searchSimilarItems(String hashValue, int resultSize) {

        HashMap<String, Double> map = lshService.searchLSH(hashValue);

        //해밍 거리를 기준으로 내림차 정렬
        ArrayList<String> keySet = new ArrayList<>(map.keySet());
        keySet.sort((o1, o2) -> map.get(o1).compareTo(map.get(o2)));

        //LSH에서 탐색한 상품 후보군 중, 유사도가 높은 상위 {resultSize}개의 상품 정보를 가져옴
        List<SearchResult> results = searchMapper.
                findItemCandidates(
                        //만약 resultSize가 상품 후보군의 전체 수보다 크다면 -> 전체 상품 정보 가져옴
                        keySet.subList(0, (resultSize > keySet.size()) ? keySet.size() : resultSize)
                );

        ExecutorService executor = Executors.newFixedThreadPool(10); // 최대 10개의 스레드를 사용
        List<CompletableFuture<Void>> futures = results.stream()
                .map(item -> CompletableFuture.runAsync(() -> {
                    try {
                        //String url = imgCtrl.cropAndResizeImage(item.getImageUrl(), 400, 300, 1);
                        //item.setImageUrl(url);
                        item.setHammingDistance(1 - map.get(item.getImageUuid()));
                        log.info("hamming {}", item.getHammingDistance());

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, executor))  // Executor를 명시적으로 지정
                .collect(Collectors.toList());

        // 모든 작업이 완료될 때까지 기다림
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        executor.shutdown(); // 작업 완료 후 Executor 종료

        results.sort((o1, o2) -> o2.getHammingDistance().compareTo(o1.getHammingDistance()));

        return results;
    }

}
