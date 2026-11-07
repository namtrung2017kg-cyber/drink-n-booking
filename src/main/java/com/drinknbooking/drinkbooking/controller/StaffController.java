package com.drinknbooking.drinkbooking.controller;

import com.drinknbooking.drinkbooking.entity.Booking;
import com.drinknbooking.drinkbooking.entity.BookingStatus;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @GetMapping("/schedule")
    public String schedule(Principal principal, Model model) {
        User me = userRepository.findByEmail(principal.getName()).orElseThrow();
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        List<Booking> all = bookingRepository.findByStaffIdOrderByBookingTimeAsc(me.getId());
        List<Booking> todo = all.stream()
                .filter(b -> b.getStatus() != BookingStatus.COMPLETED && b.getStatus() != BookingStatus.CANCELLED)
                .filter(b -> !b.getBookingTime().isBefore(startOfToday) || b.getStatus() == BookingStatus.IN_PROGRESS)
                .toList();
        List<Booking> done = all.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .toList();
        model.addAttribute("todo", todo);
        model.addAttribute("done", done);
        return "staff/schedule";
    }

    @PostMapping("/bookings/{id}/status")
    public String status(@PathVariable("id") Long id, @RequestParam("to") BookingStatus to,
                         Principal principal, RedirectAttributes ra) {
        Booking b = bookingRepository.findById(id).orElseThrow();
        if (b.getStaff() == null || !b.getStaff().getEmail().equals(principal.getName())) {
            ra.addFlashAttribute("error", "Lịch này không được phân công cho bạn");
            return "redirect:/staff/schedule";
        }
        String err = bookingService.changeStatus(b, to, false);
        if (err != null) ra.addFlashAttribute("error", err);
        else ra.addFlashAttribute("msg", "Đã chuyển trạng thái sang \"" + to.getLabel() + "\"");
        return "redirect:/staff/schedule";
    }
}