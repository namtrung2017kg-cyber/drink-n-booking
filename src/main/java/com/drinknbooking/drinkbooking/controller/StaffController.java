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
import java.util.Comparator;
import java.util.List;

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

    @GetMapping("/schedule")
    public String schedule(Principal principal, Model model) {
        User me = userRepository.findByEmail(principal.getName()).orElseThrow();
        List<Booking> all = bookingRepository.findByStaffIdOrderByCreatedAtAsc(me.getId());
        List<Booking> todo = all.stream()
                .filter(b -> b.getStatus() != BookingStatus.COMPLETED && b.getStatus() != BookingStatus.CANCELLED)
                .sorted(Comparator.comparing(this::dueTime))
                .toList();
        List<Booking> done = all.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .toList();
        model.addAttribute("pending",
                bookingRepository.findByLargeOrderFalseAndStatusOrderByCreatedAtAsc(BookingStatus.NEW));
        model.addAttribute("todo", todo);
        model.addAttribute("done", done);
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