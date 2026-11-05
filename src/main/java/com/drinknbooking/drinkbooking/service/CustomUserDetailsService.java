package com.drinknbooking.drinkbooking.service;

import com.drinknbooking.drinkbooking.entity.User;
import com.drinknbooking.drinkbooking.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User u = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản"));
        return org.springframework.security.core.userdetails.User
                .withUsername(u.getEmail())
                .password(u.getPassword())
                .roles(u.getRole().name())
                .disabled(!u.isEnabled())
                .build();
    }
}