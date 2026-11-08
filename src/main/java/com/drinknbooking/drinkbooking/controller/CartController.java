package com.drinknbooking.drinkbooking.controller;

import com.drinknbooking.drinkbooking.entity.Drink;
import com.drinknbooking.drinkbooking.repository.DrinkRepository;
import com.drinknbooking.drinkbooking.service.BookingService;
import com.drinknbooking.drinkbooking.service.Cart;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/cart")
public class CartController {
    private final Cart cart;
    private final DrinkRepository drinkRepository;

    public CartController(Cart cart, DrinkRepository drinkRepository) {
        this.cart = cart;
        this.drinkRepository = drinkRepository;
    }

    @GetMapping
    public String view(Model model) {
        List<Cart.Line> lines = cart.lines();
        int cups = lines.stream().mapToInt(Cart.Line::getQuantity).sum();
        long money = lines.stream().mapToLong(Cart.Line::getLineTotal).sum();
        model.addAttribute("lines", lines);
        model.addAttribute("totalCups", cups);
        model.addAttribute("totalMoney", money);
        model.addAttribute("largeOrder", cups > BookingService.BULK_THRESHOLD);
        model.addAttribute("threshold", BookingService.BULK_THRESHOLD);
        model.addAttribute("maxCups", BookingService.MAX_CUPS);
        return "cart";
    }

    @PostMapping("/add")
    public String add(@RequestParam("drinkId") Long drinkId,
                      @RequestParam(name = "quantity", defaultValue = "1") int quantity,
                      @RequestParam(name = "from", defaultValue = "cart") String from,
                      RedirectAttributes ra) {
        String back = "menu".equals(from) ? "redirect:/menu" : "redirect:/cart";
        Drink d = drinkRepository.findById(drinkId).filter(Drink::isAvailable).orElse(null);
        if (d == null) {
            ra.addFlashAttribute("error", "Đồ uống không còn được bán");
            return back;
        }
        if (quantity < 1) {
            ra.addFlashAttribute("error", "Số lượng tối thiểu là 1");
            return back;
        }
        if (cart.totalCups() + quantity > BookingService.MAX_CUPS) {
            ra.addFlashAttribute("error", "Mỗi đơn tối đa " + BookingService.MAX_CUPS + " ly");
            return back;
        }
        cart.add(drinkId, quantity);
        ra.addFlashAttribute("msg", "Đã thêm " + quantity + " " + d.getName() + " vào giỏ hàng");
        return back;
    }

    @PostMapping("/update")
    public String update(@RequestParam("drinkId") Long drinkId,
                         @RequestParam("quantity") int quantity,
                         RedirectAttributes ra) {
        int others = cart.totalCups() - cart.quantityOf(drinkId);
        if (others + Math.max(quantity, 0) > BookingService.MAX_CUPS) {
            ra.addFlashAttribute("error", "Mỗi đơn tối đa " + BookingService.MAX_CUPS + " ly");
            return "redirect:/cart";
        }
        cart.set(drinkId, quantity);
        return "redirect:/cart";
    }

    @PostMapping("/remove")
    public String remove(@RequestParam("drinkId") Long drinkId) {
        cart.remove(drinkId);
        return "redirect:/cart";
    }

    @PostMapping("/clear")
    public String clear() {
        cart.clear();
        return "redirect:/cart";
    }
}