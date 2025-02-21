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

    public Long insertItem(Item itemForm,
                           MultipartFile file,
                           String palette) throws IOException {
        //이미지 특징 추출 후 저장
        String imgUuid = "";
        if (!file.isEmpty()) {
            imgUuid = uploadService.uploadFile(file);
        }
/*
        // 클라이언트에서 전달 받은 이미지의 대표 색상을 저장
        ObjectMapper objectMapper = new ObjectMapper();

        int[][] paletteArray = objectMapper.readValue(palette, int[][].class);

        if (paletteArray.length > 0) {
            HashMap<String, String> map = new HashMap<>();
            ArrayList<String> duplicateCheck = new ArrayList<>();
            map.put("uuid", imgUuid);
            map.put("tag", "");

            // 최대 5x3 배열 출력
            for (int[] color : paletteArray) {
                ImageProcessing imgCtrl = new ImageProcessing();
                String colorTag = imgCtrl.getNearestColor(color);

                //컬러태그 중복 체크
                if (!duplicateCheck.contains(colorTag)) {
                    duplicateCheck.add(colorTag);
                    map.replace("tag", imgCtrl.getNearestColor(color));
                    itemMapper.insertImgColorTag(map);
                }

            }
        }

 */

        //게시물 정보 저장
        Item item = new Item(
                itemForm.getItemTitle(),
                itemForm.getItemContent(),
                itemForm.getItemPrice(),
                imgUuid
        );
        Long itemId = shopRepository.save(item);

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
