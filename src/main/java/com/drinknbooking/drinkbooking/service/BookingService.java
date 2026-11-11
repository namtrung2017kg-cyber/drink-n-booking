package com.drinknbooking.drinkbooking.service;

import com.drinknbooking.drinkbooking.entity.Booking;
import com.drinknbooking.drinkbooking.entity.BookingStatus;
import com.drinknbooking.drinkbooking.entity.User;
import com.drinknbooking.drinkbooking.repository.BookingRepository;
import org.springframework.stereotype.Service;

@Service
public class BookingService {
    public static final int BULK_THRESHOLD = 30;
    public static final int MAX_CUPS = 200;

    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    // Luật chuyển trạng thái theo từng đơn
    public boolean canMove(Booking b, BookingStatus to, boolean admin) {
        if (b.isLargeOrder()) return canMove(b.getStatus(), to, admin);
        return switch (b.getStatus()) {
            case NEW, CONFIRMED -> to == BookingStatus.IN_PROGRESS || (admin && to == BookingStatus.CANCELLED);
            case IN_PROGRESS -> to == BookingStatus.COMPLETED;
            default -> false;
        };
    }

    // Luật của đơn lớn
    public boolean canMove(BookingStatus from, BookingStatus to, boolean admin) {
        return switch (from) {
            case NEW -> admin && (to == BookingStatus.CONFIRMED || to == BookingStatus.CANCELLED);
            case CONFIRMED -> to == BookingStatus.IN_PROGRESS || (admin && to == BookingStatus.CANCELLED);
            case IN_PROGRESS -> to == BookingStatus.COMPLETED;
            default -> false;
        };
    }

    // Tên nút bấm
    public String actionLabel(Booking b, BookingStatus to) {
        if (to == BookingStatus.CANCELLED) return "Hủy đơn";
        if (!b.isLargeOrder()) {
            return to == BookingStatus.IN_PROGRESS ? "Xác nhận nhận đơn" : "Đã làm xong";
        }
        return switch (to) {
            case CONFIRMED -> "Xác nhận đơn";
            case IN_PROGRESS -> "Bắt đầu";
            default -> "Hoàn thành";
        };
    }

    private boolean staffBusy(Booking b, User staff) {
        if (!b.isLargeOrder() || b.getBookingTime() == null) return false;
        return bookingRepository.findByStaffIdOrderByCreatedAtAsc(staff.getId()).stream()
                .anyMatch(o -> !o.getId().equals(b.getId())
                        && o.isLargeOrder()
                        && o.getStatus() != BookingStatus.CANCELLED
                        && o.getBookingTime() != null
                        && o.getBookingTime().isEqual(b.getBookingTime()));
    }

    public String changeStatus(Booking b, BookingStatus to, boolean admin) {
        if (!canMove(b, to, admin)) {
            return "Không thể chuyển từ \"" + b.labelOf(b.getStatus()) + "\" sang \"" + b.labelOf(to) + "\"";
        }
        if (to == BookingStatus.CONFIRMED) {
            if (b.getStaff() == null) {
                return "Hãy chỉ định trưởng đội trước khi xác nhận đơn lớn";
            }
            if (staffBusy(b, b.getStaff())) {
                return b.getStaff().getFullName()
                        + " đã phụ trách đơn lớn khác vào giờ này, hãy đổi trưởng đội trước khi xác nhận";
            }
        }
        b.setStatus(to);
        bookingRepository.save(b);
        return null;
    }

    // Nhân viên bấm "Xác nhận nhận đơn" cho đơn thường: tự thành người phụ trách
    public String acceptByStaff(Booking b, User staff) {
        if (b.isLargeOrder()) {
            return "Đơn lớn do admin chỉ định trưởng đội";
        }
        if (b.getStatus() != BookingStatus.NEW) {
            return "Đơn này đã có người nhận hoặc không còn chờ xác nhận";
        }
        b.setStaff(staff);
        b.setStatus(BookingStatus.IN_PROGRESS);
        bookingRepository.save(b);
        return null;
    }

    // Chỉ đơn lớn mới có chỉ định trưởng đội
    public String assignStaff(Booking b, User staff) {
        if (!b.isLargeOrder()) {
            return "Đơn thường không cần phân công, nhân viên sẽ tự xác nhận nhận đơn";
        }
        if (b.getStatus() != BookingStatus.NEW && b.getStatus() != BookingStatus.CONFIRMED) {
            return "Không thể đổi trưởng đội của đơn này";
        }
        if (staff == null && b.getStatus() == BookingStatus.CONFIRMED) {
            return "Đơn đã xác nhận phải có trưởng đội";
        }
        if (staff != null && staffBusy(b, staff)) {
            return "Nhân viên này đã phụ trách đơn lớn khác vào giờ đó";
        }
        b.setStaff(staff);
        bookingRepository.save(b);
        return null;
    }
}