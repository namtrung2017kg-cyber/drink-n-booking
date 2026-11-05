package com.drinknbooking.drinkbooking.controller;

import com.drinknbooking.drinkbooking.entity.Category;
import com.drinknbooking.drinkbooking.repository.CategoryRepository;
import com.drinknbooking.drinkbooking.repository.DrinkRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {
    private final CategoryRepository categoryRepository;
    private final DrinkRepository drinkRepository;

    public AdminCategoryController(CategoryRepository categoryRepository, DrinkRepository drinkRepository) {
        this.categoryRepository = categoryRepository;
        this.drinkRepository = drinkRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/categories";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("category", new Category());
        return "admin/category-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable("id") Long id, Model model) {
        model.addAttribute("category", categoryRepository.findById(id).orElseThrow());
        return "admin/category-form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("category") Category category,
                       BindingResult br, RedirectAttributes ra) {
        if (category.getName() != null) {
            categoryRepository.findByName(category.getName().trim()).ifPresent(existing -> {
                if (!existing.getId().equals(category.getId()))
                    br.rejectValue("name", "dup", "Tên loại đã tồn tại");
            });
        }
        if (br.hasErrors()) return "admin/category-form";
        category.setName(category.getName().trim());
        categoryRepository.save(category);
        ra.addFlashAttribute("msg", "Đã lưu loại đồ uống");
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") Long id, RedirectAttributes ra) {
        if (drinkRepository.existsByCategoryId(id)) {
            ra.addFlashAttribute("error", "Không thể xóa: loại này vẫn còn đồ uống");
        } else {
            categoryRepository.deleteById(id);
            ra.addFlashAttribute("msg", "Đã xóa loại đồ uống");
        }
        return "redirect:/admin/categories";
    }
}