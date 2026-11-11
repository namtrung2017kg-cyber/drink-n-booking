package com.drinknbooking.drinkbooking.controller;

import com.drinknbooking.drinkbooking.entity.Booking;
import com.drinknbooking.drinkbooking.entity.BookingStatus;
import com.drinknbooking.drinkbooking.entity.Role;
import com.drinknbooking.drinkbooking.entity.User;
import com.drinknbooking.drinkbooking.repository.BookingRepository;
import com.drinknbooking.drinkbooking.repository.UserRepository;
import com.drinknbooking.drinkbooking.service.BookingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/staff")
public class StaffController {
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final BookingService bookingService;

    public StaffController(BookingRepository bookingRepository, UserRepository userRepository,
                           BookingService bookingService) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.bookingService = bookingService;
    }

    private LocalDateTime dueTime(Booking b) {
        if (b.isLargeOrder() && b.getBookingTime() != null) return b.getBookingTime();
        return b.getCreatedAt() != null ? b.getCreatedAt() : LocalDateTime.MIN;
    }

    private boolean isActive(Booking b) {
        return b.getStatus() != BookingStatus.COMPLETED && b.getStatus() != BookingStatus.CANCELLED;
    }

    @GetMapping("/schedule")
    public String schedule(Principal principal, Model model) {
        User me = userRepository.findByEmail(principal.getName()).orElseThrow();
        List<Booking> all = bookingRepository.findByStaffIdOrderByCreatedAtAsc(me.getId());
        List<Booking> todo = all.stream()
                .filter(this::isActive)
                .sorted(Comparator.comparing(this::dueTime))
                .toList();
        List<Booking> done = all.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .toList();
        // Đơn thường chờ nhận: chỉ hiện đơn chưa ai được giao
        List<Booking> pending = bookingRepository
                .findByLargeOrderFalseAndStatusOrderByCreatedAtAsc(BookingStatus.NEW).stream()
                .filter(b -> b.getStaff() == null)
                .toList();

        // Bảng chỉ xem: ai đang phụ trách bao nhiêu đơn
        List<Map<String, Object>> overview = new ArrayList<>();
        for (User s : userRepository.findByRoleAndEnabledTrueOrderByIdAsc(Role.STAFF)) {
            List<Booking> list = bookingRepository.findByStaffIdOrderByCreatedAtAsc(s.getId());
            List<Booking> active = list.stream().filter(this::isActive).toList();
            long doneCount = list.stream().filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", s.getFullName());
            row.put("me", s.getId().equals(me.getId()));
            row.put("active", active.size());
            row.put("done", doneCount);
            row.put("ids", active.stream().map(b -> "#" + b.getId()).collect(Collectors.joining(", ")));
            overview.add(row);
        }

        model.addAttribute("pending", pending);
        model.addAttribute("todo", todo);
        model.addAttribute("done", done);
        model.addAttribute("overview", overview);
        return "staff/schedule";
    }

    @PostMapping("/bookings/{id}/accept")
    public String accept(@PathVariable("id") Long id, Principal principal, RedirectAttributes ra) {
        User me = userRepository.findByEmail(principal.getName()).orElseThrow();
        if (me.getRole() != Role.STAFF) {
            ra.addFlashAttribute("error", "Chỉ nhân viên mới nhận đơn ở trang này. Admin hãy xác nhận trong Quản lý đơn");
            return "redirect:/staff/schedule";
        }
        Booking b = bookingRepository.findById(id).orElseThrow();
        String err = bookingService.acceptByStaff(b, me);
        if (err != null) ra.addFlashAttribute("error", err);
        else ra.addFlashAttribute("msg", "Đã nhận đơn #" + id + ", đơn chuyển sang \"" + b.getStatusLabel() + "\"");
        return "redirect:/staff/schedule";
    }

    @PostMapping("/bookings/{id}/status")
    public String status(@PathVariable("id") Long id, @RequestParam("to") BookingStatus to,
                         Principal principal, RedirectAttributes ra) {
        Booking b = bookingRepository.findById(id).orElseThrow();
        if (b.getStaff() == null || !b.getStaff().getEmail().equals(principal.getName())) {
            ra.addFlashAttribute("error", "Đơn này không phải do bạn phụ trách");
            return "redirect:/staff/schedule";
        }
        String err = bookingService.changeStatus(b, to, false);
        if (err != null) ra.addFlashAttribute("error", err);
        else ra.addFlashAttribute("msg", "Đã chuyển trạng thái sang \"" + b.getStatusLabel() + "\"");
        return "redirect:/staff/schedule";
    }
}