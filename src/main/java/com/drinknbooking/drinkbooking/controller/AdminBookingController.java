package com.drinknbooking.drinkbooking.controller;

import com.drinknbooking.drinkbooking.entity.Booking;
import com.drinknbooking.drinkbooking.entity.BookingStatus;
import com.drinknbooking.drinkbooking.entity.Role;
import com.drinknbooking.drinkbooking.entity.User;
import com.drinknbooking.drinkbooking.repository.BookingRepository;
import com.drinknbooking.drinkbooking.repository.UserRepository;
import com.drinknbooking.drinkbooking.service.BookingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/orders")
public class AdminBookingController {
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final BookingService bookingService;

    public AdminBookingController(BookingRepository bookingRepository, UserRepository userRepository,
                                  BookingService bookingService) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.bookingService = bookingService;
    }

    @GetMapping
    public String list(@RequestParam(name = "keyword", defaultValue = "") String keyword,
                       @RequestParam(name = "status", required = false) BookingStatus status,
                       @RequestParam(name = "type", defaultValue = "") String type,
                       @RequestParam(name = "date", required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       @RequestParam(name = "page", defaultValue = "0") int page,
                       Model model) {
        Boolean large = "large".equals(type) ? Boolean.TRUE : ("normal".equals(type) ? Boolean.FALSE : null);
        LocalDateTime from = date != null ? date.atStartOfDay() : LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime to = date != null ? date.plusDays(1).atStartOfDay() : LocalDateTime.of(2100, 1, 1, 0, 0);
        Page<Booking> result = bookingRepository.searchAdmin(keyword.trim(), status, large, from, to,
                PageRequest.of(Math.max(page, 0), 5, Sort.by("id").ascending()));
        model.addAttribute("bookings", result);
        model.addAttribute("statuses", BookingStatus.values());
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("type", type);
        model.addAttribute("date", date);
        return "admin/bookings";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        Booking b = bookingRepository.findById(id).orElseThrow();
        model.addAttribute("booking", b);
        model.addAttribute("staffList", userRepository.findByRoleAndEnabledTrueOrderByIdAsc(Role.STAFF));
        List<BookingStatus> next = new ArrayList<>();
        for (BookingStatus s : BookingStatus.values()) {
            if (bookingService.canMove(b.getStatus(), s, true)) next.add(s);
        }
        model.addAttribute("nextStatuses", next);
        return "admin/booking-detail";
    }

    @PostMapping("/{id}/assign")
    public String assign(@PathVariable("id") Long id,
                         @RequestParam(name = "staffId", required = false) Long staffId,
                         RedirectAttributes ra) {
        Booking b = bookingRepository.findById(id).orElseThrow();
        User staff = null;
        if (staffId != null) {
            staff = userRepository.findById(staffId)
                    .filter(u -> u.getRole() == Role.STAFF && u.isEnabled()).orElse(null);
            if (staff == null) {
                ra.addFlashAttribute("error", "Nhân viên không hợp lệ");
                return "redirect:/admin/orders/" + id;
            }
        }
        String err = bookingService.assignStaff(b, staff);
        if (err != null) ra.addFlashAttribute("error", err);
        else ra.addFlashAttribute("msg", "Đã cập nhật người phụ trách");
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/{id}/status")
    public String status(@PathVariable("id") Long id, @RequestParam("to") BookingStatus to,
                         RedirectAttributes ra) {
        Booking b = bookingRepository.findById(id).orElseThrow();
        String err = bookingService.changeStatus(b, to, true);
        if (err != null) ra.addFlashAttribute("error", err);
        else ra.addFlashAttribute("msg", "Đã chuyển trạng thái sang \"" + to.getLabel() + "\"");
        return "redirect:/admin/orders/" + id;
    }
}