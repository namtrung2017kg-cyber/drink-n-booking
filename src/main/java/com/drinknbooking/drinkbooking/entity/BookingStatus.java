package com.drinknbooking.drinkbooking.entity;

public enum BookingStatus {
    NEW("Mới tạo"),
    CONFIRMED("Đã xác nhận"),
    IN_PROGRESS("Đang thực hiện"),
    COMPLETED("Hoàn thành"),
    CANCELLED("Đã hủy");

    private final String label;

    BookingStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}