package com.drinknbooking.drinkbooking.controller;

import com.drinknbooking.drinkbooking.dto.RegisterForm;
import com.drinknbooking.drinkbooking.entity.Role;
import com.drinknbooking.drinkbooking.entity.User;
import com.drinknbooking.drinkbooking.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    public AuthController(UserRepository userRepository, PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("form", new RegisterForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form, BindingResult br) {
        if (userRepository.existsByEmail(form.getEmail()))
            br.rejectValue("email", "dup", "Email đã được đăng ký");
        if (br.hasErrors()) return "register";
        User u = new User();
        u.setFullName(form.getFullName().trim());
        u.setEmail(form.getEmail().trim());
        u.setPhone(form.getPhone());
        u.setPassword(encoder.encode(form.getPassword()));
        u.setRole(Role.CUSTOMER);
        userRepository.save(u);
        return "redirect:/login?registered";
    }
}