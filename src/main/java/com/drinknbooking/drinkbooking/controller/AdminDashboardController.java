package com.drinknbooking.drinkbooking.controller;

import com.drinknbooking.drinkbooking.entity.Booking;
import com.drinknbooking.drinkbooking.entity.BookingItem;
import com.drinknbooking.drinkbooking.entity.BookingStatus;
import com.drinknbooking.drinkbooking.entity.Role;
import com.drinknbooking.drinkbooking.entity.User;
import com.drinknbooking.drinkbooking.repository.BookingRepository;
import com.drinknbooking.drinkbooking.repository.DrinkRepository;
import com.drinknbooking.drinkbooking.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class AdminDashboardController {
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final DrinkRepository drinkRepository;

    public AdminDashboardController(BookingRepository bookingRepository, UserRepository userRepository,
                                    DrinkRepository drinkRepository) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.drinkRepository = drinkRepository;
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(@RequestParam(name = "mode", defaultValue = "day") String mode, Model model) {
        List<Booking> all = bookingRepository.findAll();
        LocalDate today = LocalDate.now();
        boolean monthMode = "month".equals(mode);

        long newCount = all.stream().filter(b -> b.getStatus() == BookingStatus.NEW).count();
        long largeCount = all.stream()
                .filter(b -> b.isLargeOrder() && b.getStatus() != BookingStatus.CANCELLED).count();
        long todayCount = all.stream()
                .filter(b -> b.isLargeOrder()
                        && b.getStatus() != BookingStatus.CANCELLED
                        && b.getBookingTime() != null
                        && b.getBookingTime().toLocalDate().equals(today))
                .count();
        long revenue = all.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .mapToLong(this::money).sum();

        List<PeriodStat> rows = new ArrayList<>();
        if (monthMode) {
            YearMonth now = YearMonth.now();
            for (int i = 5; i >= 0; i--) {
                YearMonth ym = now.minusMonths(i);
                List<Booking> inPeriod = all.stream()
                        .filter(b -> YearMonth.from(created(b)).equals(ym)).toList();
                rows.add(period(ym.format(DateTimeFormatter.ofPattern("MM/yyyy")), inPeriod));
            }
        } else {
            for (int i = 13; i >= 0; i--) {
                LocalDate d = today.minusDays(i);
                List<Booking> inPeriod = all.stream()
                        .filter(b -> created(b).toLocalDate().equals(d)).toList();
                rows.add(period(d.format(DateTimeFormatter.ofPattern("dd/MM")), inPeriod));
            }
        }

        List<StatusStat> statuses = new ArrayList<>();
        for (BookingStatus s : BookingStatus.values()) {
            long c = all.stream().filter(b -> b.getStatus() == s).count();
            statuses.add(new StatusStat(s.getLabel(), s.getBadge(), c));
        }

        Map<String, DrinkStat> map = new LinkedHashMap<>();
        for (Booking b : all) {
            if (b.getStatus() == BookingStatus.CANCELLED) continue;
            for (BookingItem it : b.getItems()) {
                DrinkStat ds = map.computeIfAbsent(it.getDrink().getName(), DrinkStat::new);
                ds.add(it.getQuantity(), b.getStatus() == BookingStatus.COMPLETED ? it.getLineTotal() : 0L);
            }
        }
        List<DrinkStat> topDrinks = map.values().stream()
                .sorted(Comparator.comparingLong(DrinkStat::getQuantity).reversed())
                .limit(5).toList();

        List<User> users = userRepository.findAll();
        long customers = users.stream().filter(u -> u.getRole() == Role.CUSTOMER).count();
        long staffs = users.stream().filter(u -> u.getRole() == Role.STAFF).count();

        model.addAttribute("mode", monthMode ? "month" : "day");
        model.addAttribute("totalBookings", all.size());
        model.addAttribute("newCount", newCount);
        model.addAttribute("largeCount", largeCount);
        model.addAttribute("todayCount", todayCount);
        model.addAttribute("revenue", revenue);
        model.addAttribute("customers", customers);
        model.addAttribute("staffs", staffs);
        model.addAttribute("drinkCount", drinkRepository.count());
        model.addAttribute("rows", rows);
        model.addAttribute("labels", rows.stream().map(PeriodStat::getLabel).toList());
        model.addAttribute("counts", rows.stream().map(PeriodStat::getCount).toList());
        model.addAttribute("revenues", rows.stream().map(PeriodStat::getRevenue).toList());
        model.addAttribute("statuses", statuses);
        model.addAttribute("topDrinks", topDrinks);
        return "admin/dashboard";
    }

    private long money(Booking b) {
        return b.getTotalPrice() == null ? 0L : b.getTotalPrice();
    }

    private LocalDateTime created(Booking b) {
        if (b.getCreatedAt() != null) return b.getCreatedAt();
        return b.getBookingTime() != null ? b.getBookingTime() : LocalDateTime.now();
    }

    private PeriodStat period(String label, List<Booking> list) {
        long count = list.stream().filter(b -> b.getStatus() != BookingStatus.CANCELLED).count();
        long rev = list.stream().filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .mapToLong(this::money).sum();
        return new PeriodStat(label, count, rev);
    }

    public static class PeriodStat {
        private final String label;
        private final long count;
        private final long revenue;

        public PeriodStat(String label, long count, long revenue) {
            this.label = label;
            this.count = count;
            this.revenue = revenue;
        }

        public String getLabel() { return label; }
        public long getCount() { return count; }
        public long getRevenue() { return revenue; }
    }

    public static class StatusStat {
        private final String label;
        private final String badge;
        private final long count;

        public StatusStat(String label, String badge, long count) {
            this.label = label;
            this.badge = badge;
            this.count = count;
        }

        public String getLabel() { return label; }
        public String getBadge() { return badge; }
        public long getCount() { return count; }
    }

    public static class DrinkStat {
        private final String name;
        private long quantity;
        private long revenue;

        public DrinkStat(String name) {
            this.name = name;
        }

        public void add(long qty, long rev) {
            this.quantity += qty;
            this.revenue += rev;
        }

        public String getName() { return name; }
        public long getQuantity() { return quantity; }
        public long getRevenue() { return revenue; }
    }
}