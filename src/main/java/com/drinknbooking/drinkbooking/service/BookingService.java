
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

    public boolean canMove(BookingStatus from, BookingStatus to, boolean admin) {
        return switch (from) {
            case NEW -> admin && (to == BookingStatus.CONFIRMED || to == BookingStatus.CANCELLED);
            case CONFIRMED -> to == BookingStatus.IN_PROGRESS || (admin && to == BookingStatus.CANCELLED);
            case IN_PROGRESS -> to == BookingStatus.COMPLETED;
            default -> false;
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
        if (!canMove(b.getStatus(), to, admin)) {
            return "Không thể chuyển từ \"" + b.getStatus().getLabel() + "\" sang \"" + to.getLabel() + "\"";
        }
        if (to == BookingStatus.CONFIRMED) {
            if (b.getStaff() == null) {
                return b.isLargeOrder()
                        ? "Hãy chỉ định trưởng đội trước khi xác nhận đơn lớn"
                        : "Hãy phân công nhân viên trước khi xác nhận";
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
    public String assignStaff(Booking b, User staff) {
        if (b.getStatus() != BookingStatus.NEW && b.getStatus() != BookingStatus.CONFIRMED) {
            return "Không thể đổi người phụ trách của đơn này";
        }
        if (staff == null && b.getStatus() == BookingStatus.CONFIRMED) {
            return "Đơn đã xác nhận phải có người phụ trách";
        }
        if (staff != null && staffBusy(b, staff)) {
            return "Nhân viên này đã phụ trách đơn lớn khác vào giờ đó";
        }
        b.setStaff(staff);
        bookingRepository.save(b);
        return null;
    }
}