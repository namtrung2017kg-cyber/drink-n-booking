package com.drinknbooking.drinkbooking.controller;

import com.drinknbooking.drinkbooking.dto.CheckoutForm;
import com.drinknbooking.drinkbooking.entity.Booking;
import com.drinknbooking.drinkbooking.entity.BookingItem;
import com.drinknbooking.drinkbooking.entity.BookingStatus;
import com.drinknbooking.drinkbooking.entity.Role;
import com.drinknbooking.drinkbooking.entity.User;
import com.drinknbooking.drinkbooking.repository.BookingRepository;
import com.drinknbooking.drinkbooking.repository.UserRepository;
import com.drinknbooking.drinkbooking.service.BookingService;
import com.drinknbooking.drinkbooking.service.Cart;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/orders")
public class BookingController {
    private static final LocalTime OPEN = LocalTime.of(7, 0);
    private static final LocalTime LAST_SLOT = LocalTime.of(20, 30);

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final Cart cart;

    public BookingController(BookingRepository bookingRepository, UserRepository userRepository, Cart cart) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.cart = cart;
    }

    private List<String> timeSlots() {
        List<String> list = new ArrayList<>();
        for (LocalTime t = OPEN; !t.isAfter(LAST_SLOT); t = t.plusMinutes(30)) {
            list.add(t.toString());
        }
        return list;
    }

    private void fillCheckout(Model model, List<Cart.Line> lines) {
        int cups = lines.stream().mapToInt(Cart.Line::getQuantity).sum();
        long money = lines.stream().mapToLong(Cart.Line::getLineTotal).sum();
        model.addAttribute("lines", lines);
        model.addAttribute("totalCups", cups);
        model.addAttribute("totalMoney", money);
        model.addAttribute("largeOrder", cups > BookingService.BULK_THRESHOLD);
        model.addAttribute("threshold", BookingService.BULK_THRESHOLD);
        model.addAttribute("slots", timeSlots());
        model.addAttribute("staffList", userRepository.findByRoleAndEnabledTrueOrderByIdAsc(Role.STAFF));
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("maxDate", LocalDate.now().plusDays(30));
    }

    @GetMapping("/checkout")
    public String checkout(Principal principal, Model model) {
        List<Cart.Line> lines = cart.lines();
        if (lines.isEmpty()) return "redirect:/cart";
        CheckoutForm form = new CheckoutForm();
        userRepository.findByEmail(principal.getName()).ifPresent(u -> form.setPhone(u.getPhone()));
        model.addAttribute("form", form);
        fillCheckout(model, lines);
        return "booking-form";
    }

    @PostMapping("/place")
    public String place(@Valid @ModelAttribute("form") CheckoutForm form, BindingResult br,
                        Principal principal, Model model, RedirectAttributes ra) {
        List<Cart.Line> lines = cart.lines();
        if (lines.isEmpty()) return "redirect:/cart";
        int cups = lines.stream().mapToInt(Cart.Line::getQuantity).sum();
        if (cups > BookingService.MAX_CUPS) {
            ra.addFlashAttribute("error", "Mỗi đơn tối đa " + BookingService.MAX_CUPS + " ly");
            return "redirect:/cart";
        }
        boolean large = cups > BookingService.BULK_THRESHOLD;

        LocalDateTime when = null;
        User leader = null;
        if (large) {
            if (form.getDate() == null && !br.hasFieldErrors("date")) {
                br.rejectValue("date", "req", "Đơn lớn cần chọn ngày giao");
            }
            if ((form.getTime() == null || form.getTime().isBlank()) && !br.hasFieldErrors("time")) {
                br.rejectValue("time", "req", "Đơn lớn cần chọn giờ giao");
            }
            if (form.getDate() != null && form.getTime() != null && !form.getTime().isBlank()
                    && !br.hasFieldErrors("date") && !br.hasFieldErrors("time")) {
                try {
                    when = LocalDateTime.of(form.getDate(), LocalTime.parse(form.getTime()));
                } catch (DateTimeParseException e) {
                    br.rejectValue("time", "bad", "Giờ không hợp lệ");
                }
            }
            if (when != null) {
                LocalTime t = when.toLocalTime();
                boolean onSlot = t.getMinute() % 30 == 0 && t.getSecond() == 0;
                LocalDateTime now = LocalDateTime.now();
                if (t.isBefore(OPEN) || t.isAfter(LAST_SLOT) || !onSlot) {
                    br.rejectValue("time", "slot", "Giờ giao từ 07:00 đến 20:30, mỗi khung 30 phút");
                } else if (when.isBefore(now.plusHours(2))) {
                    br.rejectValue("date", "past", "Đơn lớn cần đặt trước ít nhất 2 giờ");
                } else if (when.isAfter(now.plusDays(30))) {
                    br.rejectValue("date", "far", "Chỉ được đặt trước tối đa 30 ngày");
                }
            }
            if (when != null && !br.hasErrors()) {
                List<User> staffList = userRepository.findByRoleAndEnabledTrueOrderByIdAsc(Role.STAFF);
                if (form.getStaffId() != null) {
                    Long wanted = form.getStaffId();
                    leader = staffList.stream().filter(s -> s.getId().equals(wanted)).findFirst().orElse(null);
                    if (leader == null) br.rejectValue("staffId", "staff", "Trưởng đội không hợp lệ");
                }
                if (!br.hasErrors()) {
                    if (bookingRepository.countAt(when, BookingStatus.CANCELLED) >= staffList.size()) {
                        br.rejectValue("time", "full", "Khung giờ này đã hết chỗ, vui lòng chọn giờ khác");
                    } else if (leader != null
                            && bookingRepository.countByStaffAt(leader.getId(), when, BookingStatus.CANCELLED) > 0) {
                        br.rejectValue("staffId", "busy",
                                "Trưởng đội này đã có đơn lớn vào giờ đó, hãy chọn người khác hoặc giờ khác");
                    }
                }
            }
        }

        if (br.hasErrors()) {
            fillCheckout(model, lines);
            return "booking-form";
        }

        Booking b = new Booking();
        b.setCustomer(userRepository.findByEmail(principal.getName()).orElseThrow());
        b.setAddress(form.getAddress().trim());
        b.setPhone(form.getPhone());
        b.setNote(form.getNote() == null ? null : form.getNote().trim());
        b.setLargeOrder(large);
        b.setBookingTime(large ? when : null);
        b.setStaff(large ? leader : null);
        long money = 0;
        for (Cart.Line l : lines) {
            BookingItem item = new BookingItem();
            item.setBooking(b);
            item.setDrink(l.getDrink());
            item.setQuantity(l.getQuantity());
            item.setUnitPrice(l.getDrink().getPrice());
            b.getItems().add(item);
            money += l.getLineTotal();
        }
        b.setTotalCups(cups);
        b.setTotalPrice(money);
        b.setStatus(BookingStatus.NEW);
        bookingRepository.save(b);
        cart.clear();
        ra.addFlashAttribute("msg", large
                ? "Đặt đơn lớn thành công! Cửa hàng sẽ xác nhận và giao đúng giờ bạn chọn."
                : "Đặt hàng thành công! Cửa hàng sẽ xác nhận và báo giờ giao cho bạn sau.");
        return "redirect:/orders/my";
    }

    @GetMapping("/my")
    public String my(Principal principal, Model model) {
        User u = userRepository.findByEmail(principal.getName()).orElseThrow();
        model.addAttribute("bookings", bookingRepository.findByCustomerIdOrderByCreatedAtDesc(u.getId()));
        return "my-booking";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable("id") Long id, Principal principal, RedirectAttributes ra) {
        Booking b = bookingRepository.findById(id).orElseThrow();
        if (!b.getCustomer().getEmail().equals(principal.getName())) {
            ra.addFlashAttribute("error", "Bạn không có quyền hủy đơn này");
        } else if (!b.isCancellable()) {
            ra.addFlashAttribute("error",
                    "Chỉ hủy được đơn chưa thực hiện (đơn lớn phải còn ít nhất 60 phút trước giờ giao)");
        } else {
            b.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(b);
            ra.addFlashAttribute("msg", "Đã hủy đơn");
        }
        return "redirect:/orders/my";
    }
}