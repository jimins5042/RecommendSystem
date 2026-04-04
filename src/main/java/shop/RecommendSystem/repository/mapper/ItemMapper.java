package shop.RecommendSystem.repository.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import shop.RecommendSystem.dto.ImageInfo;
import shop.RecommendSystem.dto.Item;

import java.util.List;
import java.util.Map;


@Mapper
public interface ItemMapper {

    //게시글 저장
    Long save(Item item);

    //이미지 정보 + 패턴 저장
    Long insertImageInfo(ImageInfo imageInfo);

    // 이미지 정보 대량 저장
    Long insertBulkImageInfo(List<ImageInfo> imageInfoList);

    //게시글 상세 보기
    List<Item> findById(Long id);

    //전체 게시글 리스트 탐색
    List<Item> findAll(Map<String, Long> map);

    //이미지가 있는 게시글 리스트 탐색
    List<Item> findThumbnailAll(Map<String, Object> map);

    //글 목록수 반환
    Long countItems();

    // 글 삭제
    void delete(Long id);

    void bulkInsert(List<Item> items);

    Long getShopBoardSeq();

    // 스테이징 테이블 생성
    void createFeatureStaging();

    // 스테이징 테이블에 bulk insert
    void insertFeatureStaging(List<ImageInfo> list);

    // 스테이징 → image_info MERGE
    void mergeFeatureFromStaging();

    // 스테이징 테이블 삭제
    void dropFeatureStaging();

}