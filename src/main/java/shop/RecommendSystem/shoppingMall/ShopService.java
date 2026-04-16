package shop.RecommendSystem.shoppingMall;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import shop.RecommendSystem.dto.Item;
import shop.RecommendSystem.dto.Page;
import shop.RecommendSystem.repository.mapper.ItemMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopService {
    private final ItemMapper itemMapper;
    private final ShopRepository shopRepository;
    private final UploadService uploadService;

    public Map findThumbnailAll(String category, Long page) {

        return findThumbnailAll(category, page, 8L);

    }

    public Map findThumbnailAll(String category, Long page, Long size) {
        log.info("category : " + category);

        // 페이지 번호와 크기 검증
        page = (page < 1) ? 1 : page;

        if (size == null || size <= 0) {
            size = 8L;  // 페이지당 아이템 수
        }

        //Long offset = (page - 1) * size; // 페이지 번호를 0부터 시작하도록 조정
        Long offset = page - 1; // 페이지 번호를 0부터 시작하도록 조정
        try {

            List<Item> items = shopRepository.findThumbnailAll(offset, size, category); // 데이터 조회
            Page pageDto = calPage(page, size, category);

            return Map.of(
                    "items", items,
                    "pageDto", pageDto);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Long insertItem(Item itemForm, MultipartFile[] files) throws IOException {

        boolean isUpload = false;

        Item item = Item.builder()
                .itemTitle(itemForm.getItemTitle())
                .itemContent(itemForm.getItemContent())
                .itemPrice(itemForm.getItemPrice())
                .build();

        //게시물 정보 저장
        Long itemId = shopRepository.save(item);

        //이미지 특징 추출 후 저장
        if (files != null && files.length > 0) {
            isUpload = uploadService.uploadFile(files, itemId);
        }
        if (isUpload) {
            log.info("==== 상품추가 성공 ====");
        } else {
            log.info("==== 상품추가 실패 ====");
        }

        return itemId;
    }

    public Page calPage(Long page, Long size, String category) {
        page = (page < 1) ? 1 : page;

        Long totalItems = shopRepository.countItems(category); // 총 아이템 수 (DB에서 조회)
        Long totalPages = (totalItems + size - 1) / size; // 총 페이지 수 계산
        Long pageGroup = (page - 1) / 10L + 1; // 현재 페이지 그룹 계산 (페이지당 최대 10개 페이지 번호 표시)

        // 시작 페이지와 끝 페이지 계산
        Long startPage = (pageGroup - 1) * 10 + 1;
        Long endPage = Math.min(pageGroup * 10, totalPages);

        Page pageDto = new Page(page, totalPages, startPage, endPage);
        log.info("pageDto : page={}, totalPages={}, startPage={}, endPage={}", page, totalPages, startPage, endPage);

        return pageDto;
    }


}
