package com.drinknbooking.drinkbooking.service;

import com.drinknbooking.drinkbooking.entity.Drink;
import com.drinknbooking.drinkbooking.repository.DrinkRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
@Component
@SessionScope
public class Cart {
    private final DrinkRepository drinkRepository;
    private final Map<Long, Integer> items = new LinkedHashMap<>();

    public Cart(DrinkRepository drinkRepository) {
        this.drinkRepository = drinkRepository;
    }

    public void add(Long drinkId, int qty) {
        items.merge(drinkId, qty, Integer::sum);
    }

    public void set(Long drinkId, int qty) {
        if (qty <= 0) items.remove(drinkId);
        else items.put(drinkId, qty);
    }

    public void remove(Long drinkId) {
        items.remove(drinkId);
    }

    public void clear() {
        items.clear();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int quantityOf(Long drinkId) {
        return items.getOrDefault(drinkId, 0);
    }

    public int totalCups() {
        return items.values().stream().mapToInt(Integer::intValue).sum();
    }
    public List<Line> lines() {
        List<Line> result = new ArrayList<>();
        for (Long id : new ArrayList<>(items.keySet())) {
            Drink d = drinkRepository.findById(id).filter(Drink::isAvailable).orElse(null);
            if (d == null) {
                items.remove(id);
                continue;
            }
            result.add(new Line(d, items.get(id)));
        }
        return result;
    }

    public static class Line {
        private final Drink drink;
        private final int quantity;

        public Line(Drink drink, int quantity) {
            this.drink = drink;
            this.quantity = quantity;
        }

        public Drink getDrink() { return drink; }
        public int getQuantity() { return quantity; }
        public long getLineTotal() { return drink.getPrice() * quantity; }
    }
}