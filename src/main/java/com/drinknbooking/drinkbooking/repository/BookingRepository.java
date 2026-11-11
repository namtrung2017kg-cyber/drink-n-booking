package com.drinknbooking.drinkbooking.repository;

import com.drinknbooking.drinkbooking.entity.Booking;
import com.drinknbooking.drinkbooking.entity.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Booking> findByStaffIdOrderByCreatedAtAsc(Long staffId);

    List<Booking> findByLargeOrderFalseAndStatusOrderByCreatedAtAsc(BookingStatus status);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.largeOrder = true "
            + "AND b.bookingTime = :time AND b.status <> :excluded")
    long countAt(@Param("time") LocalDateTime time, @Param("excluded") BookingStatus excluded);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.largeOrder = true AND b.staff.id = :staffId "
            + "AND b.bookingTime = :time AND b.status <> :excluded")
    long countByStaffAt(@Param("staffId") Long staffId, @Param("time") LocalDateTime time,
                        @Param("excluded") BookingStatus excluded);

    @Query("SELECT b FROM Booking b WHERE (:status IS NULL OR b.status = :status) "
            + "AND (:large IS NULL OR b.largeOrder = :large) "
            + "AND (:staffId IS NULL OR b.staff.id = :staffId) "
            + "AND b.createdAt >= :from AND b.createdAt < :to "
            + "AND (LOWER(b.customer.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR LOWER(b.customer.email) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR b.phone LIKE CONCAT('%', :keyword, '%'))")
    Page<Booking> searchAdmin(@Param("keyword") String keyword, @Param("status") BookingStatus status,
                              @Param("large") Boolean large, @Param("staffId") Long staffId,
                              @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                              Pageable pageable);
}