package shop.RecommendSystem.repository.mapper;

import org.apache.ibatis.annotations.Mapper;
import shop.RecommendSystem.dto.ImageInfo;
import shop.RecommendSystem.dto.Item;

import java.util.List;
import java.util.Map;

/*
CREATE TABLE shop_board (
    item_id INT AUTO_INCREMENT PRIMARY KEY,       -- 글 고유 ID (자동 증가)
    item_title VARCHAR(255) NOT NULL,             -- 글 제목
    item_content TEXT NOT NULL,                   -- 글 내용
    item_date DATETIME DEFAULT CURRENT_TIMESTAMP, -- 작성일자 (기본값: 현재 시간)
    item_price DECIMAL(10, 2) DEFAULT 0.00,       -- 아이템 가격
    item_image_link VARCHAR(500)                  -- 이미지 링크 (최대 500자)
);

CREATE TABLE image_info (
    image_id INT AUTO_INCREMENT PRIMARY KEY, -- 기본 키 (자동 증가)
    image_uuid VARCHAR(255) NOT NULL UNIQUE, -- UUID (고유 값)
    image_original_name VARCHAR(255) NOT NULL, -- 원래 파일 이름
    image_url TEXT NOT NULL, -- S3 이미지 URL
    image_hash_code VARCHAR(64) NOT NULL, -- 개선된 pHash를 통한 이미지 해시 값
    img_feature_value VARCHAR(256) NOT NULL, -- 이미지 특징점을 비트플래그로 저장한 다음, 16진수로 변환한 값
    image_phash_v1 VARCHAR(64) NULL, -- pHash를 통한 이미지 해시 값
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 생성 시간
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP -- 갱신 시간
);

CREATE TABLE image_color_tag (
    image_color_id INT AUTO_INCREMENT PRIMARY KEY, -- 기본 키 (자동 증가)
    image_color_uuid VARCHAR(255) NOT NULL, -- UUID (고유 값)
    color_tag VARCHAR(255) NOT NULL -- UUID (고유 값)
);
*/

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