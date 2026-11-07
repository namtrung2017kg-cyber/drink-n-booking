package com.drinknbooking.drinkbooking.repository;

import com.drinknbooking.drinkbooking.entity.Booking;
import com.drinknbooking.drinkbooking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByCustomerIdOrderByBookingTimeDesc(Long customerId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.bookingTime = :time AND b.status <> :excluded")
    long countAt(@Param("time") LocalDateTime time, @Param("excluded") BookingStatus excluded);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.staff.id = :staffId "
            + "AND b.bookingTime = :time AND b.status <> :excluded")
    long countByStaffAt(@Param("staffId") Long staffId, @Param("time") LocalDateTime time,
                        @Param("excluded") BookingStatus excluded);
}