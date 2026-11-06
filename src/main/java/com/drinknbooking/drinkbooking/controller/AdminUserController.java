package com.drinknbooking.drinkbooking.controller;

import com.drinknbooking.drinkbooking.dto.UserForm;
import com.drinknbooking.drinkbooking.entity.Role;
import com.drinknbooking.drinkbooking.entity.User;
import com.drinknbooking.drinkbooking.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    public AdminUserController(UserRepository userRepository, PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    @GetMapping
    public String list(@RequestParam(name = "keyword", defaultValue = "") String keyword,
                       @RequestParam(name = "role", required = false) Role role,
                       @RequestParam(name = "page", defaultValue = "0") int page,
                       Model model) {
        Page<User> result = userRepository.search(keyword.trim(), role,
                PageRequest.of(Math.max(page, 0), 5, Sort.by("id").ascending()));
        model.addAttribute("users", result);
        model.addAttribute("roles", Role.values());
        model.addAttribute("keyword", keyword);
        model.addAttribute("role", role);
        return "admin/users";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new UserForm());
        model.addAttribute("roles", Role.values());
        return "admin/user-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable("id") Long id, Model model) {
        User u = userRepository.findById(id).orElseThrow();
        UserForm f = new UserForm();
        f.setId(u.getId());
        f.setFullName(u.getFullName());
        f.setEmail(u.getEmail());
        f.setPhone(u.getPhone());
        f.setRole(u.getRole());
        f.setEnabled(u.isEnabled());
        model.addAttribute("form", f);
        model.addAttribute("roles", Role.values());
        return "admin/user-form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("form") UserForm form, BindingResult br,
                       Principal principal, Model model, RedirectAttributes ra) {
        boolean isNew = form.getId() == null;
        String email = form.getEmail() == null ? "" : form.getEmail().trim();
        String pw = form.getPassword();
        boolean hasPw = pw != null && !pw.isBlank();

        if (isNew && !email.isEmpty() && userRepository.existsByEmail(email))
            br.rejectValue("email", "dup", "Email đã tồn tại");
        if (isNew && !hasPw)
            br.rejectValue("password", "req", "Vui lòng nhập mật khẩu");
        if (hasPw && pw.length() < 6)
            br.rejectValue("password", "len", "Mật khẩu tối thiểu 6 ký tự");

        User u = isNew ? new User() : userRepository.findById(form.getId()).orElseThrow();
        if (!isNew && u.getEmail().equals(principal.getName())
                && (form.getRole() != Role.ADMIN || !form.isEnabled()))
            br.rejectValue("role", "self", "Không thể tự hạ quyền hoặc khóa chính mình");

        if (br.hasErrors()) {
            model.addAttribute("roles", Role.values());
            return "admin/user-form";
        }
        if (isNew) u.setEmail(email);
        u.setFullName(form.getFullName().trim());
        u.setPhone(form.getPhone());
        u.setRole(form.getRole());
        u.setEnabled(form.isEnabled());
        if (hasPw) u.setPassword(encoder.encode(pw));
        userRepository.save(u);
        ra.addFlashAttribute("msg", "Đã lưu tài khoản");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable("id") Long id, Principal principal, RedirectAttributes ra) {
        User u = userRepository.findById(id).orElseThrow();
        if (u.getEmail().equals(principal.getName())) {
            ra.addFlashAttribute("error", "Không thể khóa chính mình");
        } else {
            u.setEnabled(!u.isEnabled());
            userRepository.save(u);
            ra.addFlashAttribute("msg", u.isEnabled() ? "Đã mở khóa tài khoản" : "Đã khóa tài khoản");
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") Long id, Principal principal, RedirectAttributes ra) {
        User u = userRepository.findById(id).orElseThrow();
        if (u.getEmail().equals(principal.getName())) {
            ra.addFlashAttribute("error", "Không thể xóa chính mình");
            return "redirect:/admin/users";
        }
        try {
            userRepository.delete(u);
            ra.addFlashAttribute("msg", "Đã xóa tài khoản");
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error", "Tài khoản đã có dữ liệu liên quan, hãy khóa thay vì xóa");
        }
        return "redirect:/admin/users";
    }
}