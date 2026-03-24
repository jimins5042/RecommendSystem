package shop.RecommendSystem.shoppingMall;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import shop.RecommendSystem.dto.Item;
import shop.RecommendSystem.dto.Page;
import shop.RecommendSystem.repository.mapper.ItemMapper;
import shop.RecommendSystem.repository.ShopRepository;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopService {
    private final ItemMapper itemMapper;
    private final ShopRepository shopRepository;
    private final UploadService uploadService;

    public Long insertItem(Item itemForm, MultipartFile[] files) throws IOException {

        Item item = Item.builder()
                .itemTitle(itemForm.getItemTitle())
                .itemContent(itemForm.getItemContent())
                .itemPrice(itemForm.getItemPrice())
                .build();

        //게시물 정보 저장
        Long itemId = shopRepository.save(item);

        //이미지 특징 추출 후 저장
        if (files != null && files.length > 0) {
            boolean isUpload = uploadService.uploadFile(files, itemId);
        }

        log.info("= 상품추가 성공 =");
        return itemId;
    }

    public Page calPage(Long page) {
        page = (page < 1) ? 1 : page;
        Long size = 10L;  // 페이지당 아이템 수

        Long totalItems = shopRepository.countItems(); // 총 아이템 수 (DB에서 조회)
        Long totalPages = (totalItems + size - 1) / size; // 총 페이지 수 계산
        Long pageGroup = (page - 1) / 10L + 1; // 현재 페이지 그룹 계산 (페이지당 최대 10개 페이지 번호 표시)

        // 시작 페이지와 끝 페이지 계산
        Long startPage = (pageGroup - 1) * 10 + 1;
        Long endPage = Math.min(pageGroup * 10, totalPages);

        Page pageDto = new Page(page, totalPages, startPage, endPage);

        return pageDto;
    }


}
