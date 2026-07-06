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

    // 평가용: 카테고리별 랜덤 샘플 N개씩
    List<Item> findRandomByCategory(@org.apache.ibatis.annotations.Param("perCategory") int perCategory);

    //글 목록수 반환
    Long countItems(Map map);

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

    // ── ResNet-50 + PQ 적재용 ──
    void createResnet50Staging();

    void insertResnet50Staging(List<ImageInfo> list);

    void mergeResnet50FromStaging();

    void dropResnet50Staging();

    // ══════════════════════════════════════
    // 이관(Migration) 전용
    // ══════════════════════════════════════

    // 페이징 SELECT
    Long countShopBoard();
    List<Item> selectShopBoardPage(Map<String, Object> map);
    Long countImageInfo();
    List<ImageInfo> selectImageInfoPage(Map<String, Object> map);

    // shop_board 스테이징 + MERGE (upsert by item_id)
    void createShopBoardMigrationStaging();
    void insertShopBoardMigrationStaging(List<Item> list);
    void mergeShopBoardMigration();
    void dropShopBoardMigrationStaging();

    // image_info 스테이징 + MERGE (upsert by image_uuid, 전체 컬럼)
    void createImageInfoMigrationStaging();
    void insertImageInfoMigrationStaging(List<ImageInfo> list);
    void mergeImageInfoMigration();
    void dropImageInfoMigrationStaging();

    // image_info_vggnet 스테이징 + MERGE (upsert by image_uuid)
    Long countImageInfoVggnet();
    List<ImageInfo> selectImageInfoVggnetPage(Map<String, Object> map);
    void createImageInfoVggnetMigrationStaging();
    void insertImageInfoVggnetMigrationStaging(List<ImageInfo> list);
    void mergeImageInfoVggnetMigration();
    void dropImageInfoVggnetMigrationStaging();

    // 시퀀스 조정 (import 완료 후 nextval 재정렬) — item_id만 조정 (image_uuid는 timestamp 기반)
    Long maxItemId();
    void alterShopBoardSeq(@org.apache.ibatis.annotations.Param("startWith") long startWith);

}