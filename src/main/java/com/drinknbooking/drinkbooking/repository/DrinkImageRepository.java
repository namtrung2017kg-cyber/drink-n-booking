package com.drinknbooking.drinkbooking.repository;

import com.drinknbooking.drinkbooking.entity.DrinkImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DrinkImageRepository extends JpaRepository<DrinkImage, Long> {
}