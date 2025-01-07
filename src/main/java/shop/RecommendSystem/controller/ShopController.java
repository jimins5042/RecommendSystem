package shop.RecommendSystem.controller;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import shop.RecommendSystem.dto.Item;
import shop.RecommendSystem.dto.Page;
import shop.RecommendSystem.repository.mapper.ItemMapper;
import shop.RecommendSystem.repository.ShopRepository;
import shop.RecommendSystem.service.ShopService;
import shop.RecommendSystem.service.UploadService;

import java.io.IOException;
import java.util.List;

@Controller
@Getter
@Setter
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/shop")
public class ShopController {

    private final UploadService uploadService;
    private final ShopRepository shopRepository;
    private final ShopService shopService;
    private final ItemMapper itemMapper;

    @GetMapping("/itemList")
    public String shopItemList(
            @RequestParam(value = "page", defaultValue = "1") Long page,
            Model model) {

        // 페이지 번호와 크기 검증
        page = (page < 1) ? 1 : page;
        Long size = 10L;  // 페이지당 아이템 수
        Long offset = (page - 1) * size; // 페이지 번호를 0부터 시작하도록 조정


        List<Item> items = shopRepository.selectAll(offset, size); // 데이터 조회

        Page pageDto = shopService.calPage(page);

        // 모델에 데이터 추가
        model.addAttribute("items", items);
        model.addAttribute("pageDto", pageDto);

        return "/shop/itemList";  // 템플릿 파일 경로
    }

    @GetMapping("/main")
    public String shopMain(
            @RequestParam(value = "page", defaultValue = "1") Long page,
            Model model) {

        // 페이지 번호와 크기 검증
        page = (page < 1) ? 1 : page;
        Long size = 8L;  // 페이지당 아이템 수
        Long offset = (page - 1) * size; // 페이지 번호를 0부터 시작하도록 조정
        try {
            List<Item> items = shopRepository.findThumbnailAll(offset, size); // 데이터 조회
            Page pageDto = shopService.calPage(page);

            // 모델에 데이터 추가
            model.addAttribute("items", items);
            model.addAttribute("pageDto", pageDto);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return "/shop/itemMain";  // 템플릿 파일 경로
    }

    @GetMapping("/addItem")
    public String insertItem(Model model) {

        model.addAttribute("itemForm", new Item());
        return "/shop/insertItem";
    }

    @PostMapping("/addItem")
    public String insert(@ModelAttribute Item itemForm,
                         @RequestParam("imgFile") MultipartFile file,
                         @RequestParam("palette") String palette,
                         RedirectAttributes redirectAttributes) throws IOException {

        redirectAttributes.addAttribute("productId", shopService.insertItem(itemForm, file, palette));
        return "redirect:/shop/detail/{productId}";

    }

    @GetMapping("/detail/{id}")
    public String showItem(@PathVariable("id") Long id, Model model) {
        log.info("=== 이미지 상세 조회 ===");

        Item item = shopRepository.findById(id);

        model.addAttribute("item", item);

        return "/shop/showItem";
    }

    @GetMapping("/delete/{id}")
    public String deleteItem(@PathVariable("id") Long id, Model model) {
        shopRepository.deleteItem(id);

        return "/shop/itemList";
    }
}
