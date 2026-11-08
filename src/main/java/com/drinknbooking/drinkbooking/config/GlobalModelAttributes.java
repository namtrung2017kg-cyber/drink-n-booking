package com.drinknbooking.drinkbooking.config;

import com.drinknbooking.drinkbooking.service.Cart;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {
    private final Cart cart;

    public GlobalModelAttributes(Cart cart) {
        this.cart = cart;
    }

    @ModelAttribute("cartCount")
    public int cartCount() {
        return cart.totalCups();
    }
}