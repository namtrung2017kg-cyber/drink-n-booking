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
        if (b.getStaff() != null && !b.getStaff().getId().equals(staff.getId())) {
            return "Đơn này admin đã giao cho " + b.getStaff().getFullName();
        }
        b.setStaff(staff);
        b.setStatus(BookingStatus.IN_PROGRESS);
        bookingRepository.save(b);
        return null;
    }

    // Admin đổi người phụ trách (đơn thường) hoặc chỉ định trưởng đội (đơn lớn)
    public String assignStaff(Booking b, User staff) {
        BookingStatus st = b.getStatus();
        if (b.isLargeOrder()) {
            if (st != BookingStatus.NEW && st != BookingStatus.CONFIRMED) {
                return "Không thể đổi trưởng đội của đơn này";
            }
            if (staff == null && st == BookingStatus.CONFIRMED) {
                return "Đơn đã xác nhận phải có trưởng đội";
            }
            if (staff != null && staffBusy(b, staff)) {
                return "Nhân viên này đã phụ trách đơn lớn khác vào giờ đó";
            }
        } else {
            if (st != BookingStatus.NEW && st != BookingStatus.CONFIRMED && st != BookingStatus.IN_PROGRESS) {
                return "Chỉ đổi được người phụ trách khi đơn chưa làm xong";
            }
            if (staff == null && st == BookingStatus.IN_PROGRESS) {
                return "Đơn đang làm phải có người phụ trách";
            }
        }
        b.setStaff(staff);
        bookingRepository.save(b);
        return null;
    }
}