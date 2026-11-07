package com.drinknbooking.drinkbooking.controller;

import com.drinknbooking.drinkbooking.entity.Category;
import com.drinknbooking.drinkbooking.entity.Drink;
import com.drinknbooking.drinkbooking.entity.DrinkImage;
import com.drinknbooking.drinkbooking.repository.CategoryRepository;
import com.drinknbooking.drinkbooking.repository.DrinkImageRepository;
import com.drinknbooking.drinkbooking.repository.DrinkRepository;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Controller
@RequestMapping("/admin/drinks")
public class AdminDrinkController {
    private final DrinkRepository drinkRepository;
    private final CategoryRepository categoryRepository;
    private final DrinkImageRepository drinkImageRepository;

    public AdminDrinkController(DrinkRepository drinkRepository, CategoryRepository categoryRepository,
                                DrinkImageRepository drinkImageRepository) {
        this.drinkRepository = drinkRepository;
        this.categoryRepository = categoryRepository;
        this.drinkImageRepository = drinkImageRepository;
    }

    @GetMapping
    public String list(@RequestParam(name = "keyword", defaultValue = "") String keyword,
                       @RequestParam(name = "categoryId", required = false) Long categoryId,
                       @RequestParam(name = "page", defaultValue = "0") int page,
                       Model model) {
        Page<Drink> result = drinkRepository.search(keyword.trim(), categoryId,
                PageRequest.of(Math.max(page, 0), 5, Sort.by("id").ascending()));
        model.addAttribute("drinks", result);
        model.addAttribute("categories", categoryRepository.findAll(Sort.by("id")));
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        return "admin/drinks";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        Drink d = new Drink();
        d.setCategory(new Category());
        model.addAttribute("drink", d);
        model.addAttribute("categories", categoryRepository.findAll(Sort.by("id")));
        return "admin/drink-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable("id") Long id, Model model) {
        model.addAttribute("drink", drinkRepository.findById(id).orElseThrow());
        model.addAttribute("categories", categoryRepository.findAll(Sort.by("id")));
        return "admin/drink-form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("drink") Drink drink, BindingResult br,
                       @RequestParam(name = "imageFile", required = false) MultipartFile imageFile,
                       @RequestParam(name = "removeImage", defaultValue = "false") boolean removeImage,
                       Model model, RedirectAttributes ra) throws IOException {
        if (drink.getCategory() == null) drink.setCategory(new Category());
        Long catId = drink.getCategory().getId();
        if (catId == null) br.rejectValue("category.id", "req", "Vui lòng chọn loại đồ uống");

        boolean hasFile = imageFile != null && !imageFile.isEmpty();
        byte[] imageBytes = null;
        if (hasFile) {
            imageBytes = compress(imageFile);
            if (imageBytes == null)
                br.rejectValue("imageUrl", "img", "File không phải ảnh hợp lệ (chỉ hỗ trợ JPG, PNG, GIF)");
        }

        if (br.hasErrors()) {
            model.addAttribute("categories", categoryRepository.findAll(Sort.by("id")));
            return "admin/drink-form";
        }

        drink.setCategory(categoryRepository.findById(catId).orElseThrow());
        drink.setName(drink.getName().trim());
        if (removeImage && !hasFile) drink.setImageUrl(null);
        Drink saved = drinkRepository.save(drink);

        if (removeImage && !hasFile) {
            drinkImageRepository.deleteById(saved.getId());
        }
        if (hasFile) {
            DrinkImage img = new DrinkImage();
            img.setDrinkId(saved.getId());
            img.setData(imageBytes);
            drinkImageRepository.save(img);
            saved.setImageUrl("/drinks/" + saved.getId() + "/image?v=" + System.currentTimeMillis());
            drinkRepository.save(saved);
        }
        ra.addFlashAttribute("msg", "Đã lưu đồ uống");
        return "redirect:/admin/drinks";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            drinkRepository.deleteById(id);
            drinkImageRepository.deleteById(id);
            ra.addFlashAttribute("msg", "Đã xóa đồ uống");
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error", "Đồ uống đã có lịch đặt, hãy bỏ tích Đang bán thay vì xóa");
        }
        return "redirect:/admin/drinks";
    }

    private byte[] compress(MultipartFile file) throws IOException {
        BufferedImage src = ImageIO.read(file.getInputStream());
        if (src == null) return null;
        int w = src.getWidth();
        int h = src.getHeight();
        double scale = Math.min(1.0, 800.0 / Math.max(w, h));
        int nw = Math.max(1, (int) (w * scale));
        int nh = Math.max(1, (int) (h * scale));
        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, nw, nh);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(out, "jpg", baos);
        return baos.toByteArray();
    }
}