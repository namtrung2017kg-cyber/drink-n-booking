package com.drinknbooking.drinkbooking.controller;

import com.drinknbooking.drinkbooking.entity.User;
import com.drinknbooking.drinkbooking.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
public class PageController {
    private final UserRepository userRepository;

    public PageController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String home() { return "home"; }

    @GetMapping("/profile")
    public String profile(Principal p, Model model) {
        model.addAttribute("user", userRepository.findByEmail(p.getName()).orElseThrow());
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(Principal p, @RequestParam String fullName,
                                @RequestParam(required = false) String phone, RedirectAttributes ra) {
        if (fullName.isBlank()) {
            ra.addFlashAttribute("error", "Họ tên không được để trống");
            return "redirect:/profile";
        }
        if (phone != null && !phone.matches("^(0\\d{9})?$")) {
            ra.addFlashAttribute("error", "SĐT gồm 10 số, bắt đầu bằng 0");
            return "redirect:/profile";
        }
        User u = userRepository.findByEmail(p.getName()).orElseThrow();
        u.setFullName(fullName.trim());
        u.setPhone(phone);
        userRepository.save(u);
        ra.addFlashAttribute("msg", "Đã cập nhật thông tin");
        return "redirect:/profile";
    }
}