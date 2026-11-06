package com.drinknbooking.drinkbooking.repository;

import com.drinknbooking.drinkbooking.entity.Drink;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DrinkRepository extends JpaRepository<Drink, Long> {
    @Query("SELECT d FROM Drink d WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "AND (:categoryId IS NULL OR d.category.id = :categoryId)")
    Page<Drink> search(@Param("keyword") String keyword,
                       @Param("categoryId") Long categoryId,
                       Pageable pageable);

    @Query("SELECT d FROM Drink d WHERE d.available = true "
            + "AND LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "AND (:categoryId IS NULL OR d.category.id = :categoryId)")
    Page<Drink> searchAvailable(@Param("keyword") String keyword,
                                @Param("categoryId") Long categoryId,
                                Pageable pageable);

    boolean existsByCategoryId(Long categoryId);
}