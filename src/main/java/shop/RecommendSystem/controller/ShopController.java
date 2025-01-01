package shop.RecommendSystem.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import shop.RecommendSystem.dto.Item;
import shop.RecommendSystem.repository.ShopRepository;
import shop.RecommendSystem.repository.mapper.ItemMapper;
import shop.RecommendSystem.service.UploadService;

import java.io.IOException;

@Controller
@Getter
@Setter
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/shop")
public class ShopController {

    private final UploadService uploadService;

    private final ShopRepository shopRepository;

    private final ItemMapper itemMapper;

    @GetMapping("/main")
    public String shopMain() {
        return "ShopMain";
    }

    @GetMapping("/addItem")
    public String insertItem(Model model) {

        model.addAttribute("itemForm", new Item());
        return "/shop/insertItem";
    }

    @PostMapping("/addItem")
    public String insert(@ModelAttribute Item itemForm,
                         @RequestParam("imgFile") MultipartFile file,
                         HttpServletRequest request,
                         RedirectAttributes redirectAttributes) throws IOException {

        String imgUrl = "";
        if (!file.isEmpty()) {
            imgUrl = uploadService.uploadFile(file);
        }

        Item item = new Item(
                itemForm.getItemTitle(),
                itemForm.getItemContent(),
                itemForm.getItemPrice(),
                imgUrl
        );
        Long itemId = shopRepository.save(item);

        log.info("= 상품추가 성공 =");

        redirectAttributes.addAttribute("productId", itemId);
        return "redirect:/shop/detail/{productId}";

    }

    @GetMapping("/detail/{id}")
    public String showItem(@PathVariable("id") Long id, Model model) {
        log.info("=== 이미지 상세 조회 ===");

        Item item = shopRepository.findById(id);

        model.addAttribute("item", item);

        return "/shop/showItem";
    }

    @GetMapping("/delete")
    public String deleteItem() {
        return "ShopMain";
    }
}
