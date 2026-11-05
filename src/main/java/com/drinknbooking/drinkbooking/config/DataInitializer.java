package com.drinknbooking.drinkbooking.config;

import com.drinknbooking.drinkbooking.entity.Role;
import com.drinknbooking.drinkbooking.entity.User;
import com.drinknbooking.drinkbooking.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    private final UserRepository repo;
    private final PasswordEncoder encoder;

    public DataInitializer(UserRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        create("Quản trị viên", "admin@drink.com", "admin123", Role.ADMIN);
        create("Nhân viên A", "staff@drink.com", "staff123", Role.STAFF);
    }

    private void create(String name, String email, String pass, Role role) {
        if (repo.existsByEmail(email)) return;
        User u = new User();
        u.setFullName(name);
        u.setEmail(email);
        u.setPassword(encoder.encode(pass));
        u.setRole(role);
        repo.save(u);
    }
}