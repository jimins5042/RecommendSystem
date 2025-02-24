package shop.RecommendSystem.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import shop.RecommendSystem.dto.SearchResult;
import shop.RecommendSystem.recommend.ItemFiltering.PreFiltering;
import shop.RecommendSystem.repository.mapper.SearchMapper;
import shop.RecommendSystem.recommend.ItemFiltering.PrefixFiltering;

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

    private final PrefixFiltering prefix;
    private final PreFiltering filter;
    private final SearchMapper searchMapper;

    /**
     * pHash를 이용해 유사 이미지를 탐색하는 함수
     *
     * @param hashcode : 16진수로 변환된 pHash 값
     * @param resultSize : 반환받을 탐색 결과 갯수
     * @return : 유사도가 큰 순으로 정렬된 상품 정보 데이터
     */
    public List<SearchResult> searchSimilarItems(String hashcode, int resultSize) {

        HashMap<String, Double> map = prefix.searchSimilarItem(hashcode);
        return searchSimilarItems(map, resultSize);
    }

    /**
     * VGG16을 이용해 유사 객체 이미지를 탐색하는 함수
     *
     * @param order : Intensity 평균값의 크기 순으로 정렬된, 이미지의 레이어 번호. JSON 형식으로 되어 있음
     * @param imgFeature : 이진화된 25088bit 크기의 이미지 특징점
     * @param resultSize : 반환받을 탐색 결과 갯수
     * @return 유사도가 큰 순으로 정렬된 상품 정보 데이터
     * @throws JsonProcessingException
     */
    public List<SearchResult> searchSimilarItems(String order, byte[] imgFeature, int resultSize) throws JsonProcessingException {
        log.info("searchSimilarItems order: {}", order);
        HashMap<String, Double> map = filter.searchSimilarItem(order, imgFeature);

        return searchSimilarItems(map, resultSize);
    }

    /**
     * 상품의 uuid와 유사도가 저장된 HashMap을 바탕으로 상품 정보를 DB에서 가져오고, 유사도 순으로 정렬하여 탐색 결과를 반환
     *
     * @param map : 상품의 uuid와 유사도가 저장된 HashMap
     * @param resultSize : 반환할 탐색 결과 갯수
     * @return : 유사도가 큰 순으로 정렬된 상품 정보 데이터
     */
    public List<SearchResult> searchSimilarItems(HashMap<String, Double> map, int resultSize) {

        //유사도를 기준으로 내림차 정렬
        ArrayList<String> keySet = new ArrayList<>(map.keySet());
        keySet.sort((o1, o2) -> map.get(o2).compareTo(map.get(o1)));
        List<SearchResult> results = new ArrayList<>();

        if (keySet.size() < 1) {
            return results;
        }

        //LSH에서 탐색한 상품 후보군 중, 유사도가 높은 상위 {resultSize}개의 상품 정보를 가져옴
        results = searchMapper
                .findItemCandidates(
                        //만약 resultSize가 상품 후보군의 전체 수보다 크다면 -> 전체 상품 정보 가져옴
                        keySet.subList(0, (resultSize > keySet.size()) ? keySet.size() : resultSize)
                        //keySet.subList(0, keySet.size())
                );

        ExecutorService executor = Executors.newFixedThreadPool(10); // 최대 10개의 스레드를 사용
        List<CompletableFuture<Void>> futures = results.stream()
                .map(item -> CompletableFuture.runAsync(() -> {
                    try {
                        item.setHammingDistance(map.get(item.getImageUuid()));
                        log.info("hamming = {}", item.getHammingDistance());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, executor))  // Executor를 명시적으로 지정
                .collect(Collectors.toList());

        // 모든 작업이 완료될 때까지 기다림
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        executor.shutdown(); // 작업 완료 후 Executor 종료

        // DB에서 상품 정보를 반환 받을 때 uuid의 사전순으로 반환 받으므로, 유사도 순으로 재정렬
        results.sort((o1, o2) -> o2.getHammingDistance().compareTo(o1.getHammingDistance()));

        return results;
    }

}
