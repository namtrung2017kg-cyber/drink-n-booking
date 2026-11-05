package com.drinknbooking.drinkbooking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterForm {
    @NotBlank(message = "Vui lòng nhập họ tên")
    private String fullName;
    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không hợp lệ")
    private String email;
    @Pattern(regexp = "^(0\\d{9})?$", message = "SĐT gồm 10 số, bắt đầu bằng 0")
    private String phone;
    @NotBlank(message = "Vui lòng nhập mật khẩu")
    @Size(min = 6, message = "Mật khẩu tối thiểu 6 ký tự")
    private String password;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}