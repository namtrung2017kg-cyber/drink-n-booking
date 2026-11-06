package com.drinknbooking.drinkbooking.controller;

import com.drinknbooking.drinkbooking.repository.DrinkImageRepository;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.concurrent.TimeUnit;

@Controller
public class ImageController {
    private final DrinkImageRepository drinkImageRepository;

    public ImageController(DrinkImageRepository drinkImageRepository) {
        this.drinkImageRepository = drinkImageRepository;
    }

    @GetMapping("/drinks/{id}/image")
    public ResponseEntity<byte[]> image(@PathVariable("id") Long id) {
        return drinkImageRepository.findById(id)
                .map(img -> ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS))
                        .body(img.getData()))
                .orElse(ResponseEntity.notFound().build());
    }
}