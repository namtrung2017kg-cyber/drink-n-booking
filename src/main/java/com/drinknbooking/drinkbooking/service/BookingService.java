package com.drinknbooking.drinkbooking.service;

import com.drinknbooking.drinkbooking.entity.Booking;
import com.drinknbooking.drinkbooking.entity.BookingStatus;
import com.drinknbooking.drinkbooking.entity.User;
import com.drinknbooking.drinkbooking.repository.BookingRepository;
import org.springframework.stereotype.Service;

@Service
public class BookingService {
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

    /** Nhân viên này đã có lịch khác (chưa hủy) cùng giờ với lịch b chưa? */
    private boolean staffBusy(Booking b, User staff) {
        return bookingRepository.findByStaffIdOrderByBookingTimeAsc(staff.getId()).stream()
                .anyMatch(other -> !other.getId().equals(b.getId())
                        && other.getStatus() != BookingStatus.CANCELLED
                        && other.getBookingTime().isEqual(b.getBookingTime()));
    }

    /** Trả về null nếu thành công, ngược lại trả về nội dung lỗi. */
    public String changeStatus(Booking b, BookingStatus to, boolean admin) {
        if (!canMove(b.getStatus(), to, admin)) {
            return "Không thể chuyển từ \"" + b.getStatus().getLabel() + "\" sang \"" + to.getLabel() + "\"";
        }
        if (to == BookingStatus.CONFIRMED) {
            if (b.getStaff() == null) {
                return "Hãy phân công nhân viên trước khi xác nhận";
            }
            if (staffBusy(b, b.getStaff())) {
                return "Nhân viên " + b.getStaff().getFullName()
                        + " đã có lịch khác vào giờ này, hãy đổi nhân viên trước khi xác nhận";
            }
        }
        b.setStatus(to);
        bookingRepository.save(b);
        return null;
    }

    /** Trả về null nếu thành công, ngược lại trả về nội dung lỗi. */
    public String assignStaff(Booking b, User staff) {
        if (b.getStatus() != BookingStatus.NEW && b.getStatus() != BookingStatus.CONFIRMED) {
            return "Không thể đổi nhân viên của lịch này";
        }
        if (staff == null && b.getStatus() == BookingStatus.CONFIRMED) {
            return "Lịch đã xác nhận phải có nhân viên phụ trách";
        }
        if (staff != null && staffBusy(b, staff)) {
            return "Nhân viên này đã có lịch vào giờ đó";
        }
        b.setStaff(staff);
        bookingRepository.save(b);
        return null;
    }
}