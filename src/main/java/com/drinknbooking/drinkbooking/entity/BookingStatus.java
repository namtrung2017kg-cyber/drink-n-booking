package com.drinknbooking.drinkbooking.entity;

public enum BookingStatus {
    NEW("Mới tạo", "bg-primary"),
    CONFIRMED("Đã xác nhận", "bg-info text-dark"),
    IN_PROGRESS("Đang thực hiện", "bg-warning text-dark"),
    COMPLETED("Hoàn thành", "bg-success"),
    CANCELLED("Đã hủy", "bg-secondary");

    private final String label;
    private final String badge;

    BookingStatus(String label, String badge) {
        this.label = label;
        this.badge = badge;
    }

    public String getLabel() {
        return label;
    }

    public String getBadge() {
        return badge;
    }
}