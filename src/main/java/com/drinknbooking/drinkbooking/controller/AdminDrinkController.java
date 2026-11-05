package com.drinknbooking.drinkbooking.controller;

import com.drinknbooking.drinkbooking.entity.Category;
import com.drinknbooking.drinkbooking.entity.Drink;
import com.drinknbooking.drinkbooking.repository.CategoryRepository;
import com.drinknbooking.drinkbooking.repository.DrinkRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/drinks")
public class AdminDrinkController {
    private final DrinkRepository drinkRepository;
    private final CategoryRepository categoryRepository;

    public AdminDrinkController(DrinkRepository drinkRepository, CategoryRepository categoryRepository) {
        this.drinkRepository = drinkRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public String list(@RequestParam(name = "keyword", defaultValue = "") String keyword,
                       @RequestParam(name = "categoryId", required = false) Long categoryId,
                       @RequestParam(name = "page", defaultValue = "0") int page,
                       Model model) {
        Page<Drink> result = drinkRepository.search(keyword.trim(), categoryId,
                PageRequest.of(Math.max(page, 0), 5, Sort.by("id").descending()));
        model.addAttribute("drinks", result);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        return "admin/drinks";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        Drink d = new Drink();
        d.setCategory(new Category());
        model.addAttribute("drink", d);
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/drink-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable("id") Long id, Model model) {
        model.addAttribute("drink", drinkRepository.findById(id).orElseThrow());
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/drink-form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("drink") Drink drink, BindingResult br,
                       Model model, RedirectAttributes ra) {
        if (drink.getCategory() == null) drink.setCategory(new Category());
        Long catId = drink.getCategory().getId();
        if (catId == null) br.rejectValue("category.id", "req", "Vui lòng chọn loại đồ uống");
        if (br.hasErrors()) {
            model.addAttribute("categories", categoryRepository.findAll());
            return "admin/drink-form";
        }
        drink.setCategory(categoryRepository.findById(catId).orElseThrow());
        drink.setName(drink.getName().trim());
        drinkRepository.save(drink);
        ra.addFlashAttribute("msg", "Đã lưu đồ uống");
        return "redirect:/admin/drinks";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") Long id, RedirectAttributes ra) {
        drinkRepository.deleteById(id);
        ra.addFlashAttribute("msg", "Đã xóa đồ uống");
        return "redirect:/admin/drinks";
    }
}