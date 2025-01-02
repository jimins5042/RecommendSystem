package shop.RecommendSystem.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import shop.RecommendSystem.dto.Item;
import shop.RecommendSystem.dto.Page;
import shop.RecommendSystem.repository.ShopRepository;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopService {

    private final ShopRepository shopRepository;

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
