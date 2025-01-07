package shop.RecommendSystem.repository.mapper;

import org.apache.ibatis.annotations.Mapper;
import shop.RecommendSystem.dto.ImageInfo;
import shop.RecommendSystem.dto.Item;

import java.util.List;
import java.util.Map;

@Mapper
public interface ItemMapper {

    //게시글 저장
    void save(Item item);

    //이미지 정보 + 패턴 저장
    void insertImageInfo(ImageInfo imageInfo);

    //이미지 색상 정보 저장
    void insertImgColorTag(Map<String, String> map);

    //게시글 상세 보기
    Item findById(Long id);

    //전체 게시글 리스트 탐색
    List<Item> findAll(Map<String, Long> map);

    //이미지가 있는 게시글 리스트 탐색
    List<Item> findThumbnailAll(Map<String, Long> map);

    //글 목록수 반환
    Long countItems();

    // 글 삭제
    void delete(Long id);
}