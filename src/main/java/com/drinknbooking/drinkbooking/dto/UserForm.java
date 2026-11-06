package com.drinknbooking.drinkbooking.dto;

import com.drinknbooking.drinkbooking.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class UserForm {
    private Long id;
    @NotBlank(message = "Vui lòng nhập họ tên")
    private String fullName;
    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không hợp lệ")
    private String email;
    @Pattern(regexp = "^(0\\d{9})?$", message = "SĐT gồm 10 số, bắt đầu bằng 0")
    private String phone;
    @NotNull(message = "Vui lòng chọn vai trò")
    private Role role = Role.STAFF;
    private boolean enabled = true;
    private String password;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}