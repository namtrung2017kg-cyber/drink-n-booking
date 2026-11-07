package com.drinknbooking.drinkbooking.controller;

import com.drinknbooking.drinkbooking.dto.BookingForm;
import com.drinknbooking.drinkbooking.entity.Booking;
import com.drinknbooking.drinkbooking.entity.BookingStatus;
import com.drinknbooking.drinkbooking.entity.Drink;
import com.drinknbooking.drinkbooking.entity.Role;
import com.drinknbooking.drinkbooking.entity.User;
import com.drinknbooking.drinkbooking.repository.BookingRepository;
import com.drinknbooking.drinkbooking.repository.DrinkRepository;
import com.drinknbooking.drinkbooking.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/booking")
public class BookingController {
    private static final LocalTime OPEN = LocalTime.of(7, 0);
    private static final LocalTime LAST_SLOT = LocalTime.of(20, 30);

    private final BookingRepository bookingRepository;
    private final DrinkRepository drinkRepository;
    private final UserRepository userRepository;

    public BookingController(BookingRepository bookingRepository, DrinkRepository drinkRepository,
                             UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.drinkRepository = drinkRepository;
        this.userRepository = userRepository;
    }

    private List<String> timeSlots() {
        List<String> list = new ArrayList<>();
        for (LocalTime t = OPEN; !t.isAfter(LAST_SLOT); t = t.plusMinutes(30)) {
            list.add(t.toString());
        }
        return list;
    }

    private void fillModel(Model model, Drink drink) {
        model.addAttribute("drink", drink);
        model.addAttribute("slots", timeSlots());
        model.addAttribute("staffList", userRepository.findByRoleAndEnabledTrueOrderByIdAsc(Role.STAFF));
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("maxDate", LocalDate.now().plusDays(14));
    }

    @GetMapping("/new")
    public String newForm(@RequestParam("drinkId") Long drinkId, Model model) {
        Drink drink = drinkRepository.findById(drinkId).filter(Drink::isAvailable).orElse(null);
        if (drink == null) return "redirect:/menu";
        BookingForm form = new BookingForm();
        form.setDrinkId(drinkId);
        model.addAttribute("form", form);
        fillModel(model, drink);
        return "booking-form";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("form") BookingForm form, BindingResult br,
                         Principal principal, Model model, RedirectAttributes ra) {
        Long drinkId = form.getDrinkId() == null ? -1L : form.getDrinkId();
        Drink drink = drinkRepository.findById(drinkId).filter(Drink::isAvailable).orElse(null);
        if (drink == null) return "redirect:/menu";

        LocalDateTime when = null;
        if (!br.hasFieldErrors("date") && !br.hasFieldErrors("time")) {
            try {
                when = LocalDateTime.of(form.getDate(), LocalTime.parse(form.getTime()));
            } catch (DateTimeParseException e) {
                br.rejectValue("time", "bad", "Giờ không hợp lệ");
            }
        }

        if (when != null) {
            LocalTime t = when.toLocalTime();
            boolean onSlot = t.getMinute() % 30 == 0 && t.getSecond() == 0;
            if (t.isBefore(OPEN) || t.isAfter(LAST_SLOT) || !onSlot) {
                br.rejectValue("time", "slot", "Giờ nhận từ 07:00 đến 20:30, mỗi khung 30 phút");
            } else if (when.isBefore(LocalDateTime.now().plusMinutes(30))) {
                br.rejectValue("date", "past", "Phải đặt trước ít nhất 30 phút");
            } else if (when.isAfter(LocalDateTime.now().plusDays(14))) {
                br.rejectValue("date", "far", "Chỉ được đặt trước tối đa 14 ngày");
            }
        }

        User staff = null;
        if (when != null && !br.hasErrors()) {
            List<User> staffList = userRepository.findByRoleAndEnabledTrueOrderByIdAsc(Role.STAFF);
            if (form.getStaffId() != null) {
                Long wantedId = form.getStaffId();
                staff = staffList.stream().filter(s -> s.getId().equals(wantedId)).findFirst().orElse(null);
                if (staff == null) br.rejectValue("staffId", "staff", "Nhân viên không hợp lệ");
            }
            if (!br.hasErrors()) {
                if (bookingRepository.countAt(when, BookingStatus.CANCELLED) >= staffList.size()) {
                    br.rejectValue("time", "full", "Khung giờ này đã hết chỗ, vui lòng chọn giờ khác");
                } else if (staff != null
                        && bookingRepository.countByStaffAt(staff.getId(), when, BookingStatus.CANCELLED) > 0) {
                    br.rejectValue("staffId", "busy",
                            "Nhân viên này đã có lịch vào giờ đó, hãy chọn nhân viên hoặc giờ khác");
                }
            }
        }

        if (br.hasErrors()) {
            fillModel(model, drink);
            return "booking-form";
        }

        Booking b = new Booking();
        b.setCustomer(userRepository.findByEmail(principal.getName()).orElseThrow());
        b.setDrink(drink);
        b.setStaff(staff);
        b.setQuantity(form.getQuantity());
        b.setTotalPrice(drink.getPrice() * form.getQuantity());
        b.setBookingTime(when);
        b.setNote(form.getNote() == null ? null : form.getNote().trim());
        b.setStatus(BookingStatus.NEW);
        bookingRepository.save(b);
        ra.addFlashAttribute("msg", "Đặt lịch thành công! Cửa hàng sẽ xác nhận sớm.");
        return "redirect:/booking/my";
    }

    @GetMapping("/my")
    public String my(Principal principal, Model model) {
        User u = userRepository.findByEmail(principal.getName()).orElseThrow();
        model.addAttribute("bookings", bookingRepository.findByCustomerIdOrderByBookingTimeDesc(u.getId()));
        return "my-booking";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable("id") Long id, Principal principal, RedirectAttributes ra) {
        Booking b = bookingRepository.findById(id).orElseThrow();
        if (!b.getCustomer().getEmail().equals(principal.getName())) {
            ra.addFlashAttribute("error", "Bạn không có quyền hủy lịch này");
        } else if (!b.isCancellable()) {
            ra.addFlashAttribute("error",
                    "Chỉ hủy được lịch chưa thực hiện và còn ít nhất 60 phút trước giờ hẹn");
        } else {
            b.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(b);
            ra.addFlashAttribute("msg", "Đã hủy lịch");
        }
        return "redirect:/booking/my";
    }
}