package com.drinknbooking.drinkbooking.controller;

import com.drinknbooking.drinkbooking.entity.Drink;
import com.drinknbooking.drinkbooking.repository.CategoryRepository;
import com.drinknbooking.drinkbooking.repository.DrinkRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/menu")
public class MenuController {
    private final DrinkRepository drinkRepository;
    private final CategoryRepository categoryRepository;

    public MenuController(DrinkRepository drinkRepository, CategoryRepository categoryRepository) {
        this.drinkRepository = drinkRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public String list(@RequestParam(name = "keyword", defaultValue = "") String keyword,
                       @RequestParam(name = "categoryId", required = false) Long categoryId,
                       @RequestParam(name = "page", defaultValue = "0") int page,
                       Model model) {
        Page<Drink> result = drinkRepository.searchAvailable(keyword.trim(), categoryId,
                PageRequest.of(Math.max(page, 0), 6, Sort.by("id").ascending()));
        model.addAttribute("drinks", result);
        model.addAttribute("categories", categoryRepository.findAll(Sort.by("id")));
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        return "menu";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        Drink d = drinkRepository.findById(id).filter(Drink::isAvailable).orElse(null);
        if (d == null) return "redirect:/menu";
        model.addAttribute("drink", d);
        return "menu-detail";
    }
}