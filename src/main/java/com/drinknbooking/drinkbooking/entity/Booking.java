package com.drinknbooking.drinkbooking.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "orders")
public class Booking {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<BookingItem> items = new ArrayList<>();

    private int totalCups;
    private Long totalPrice;
    private boolean largeOrder;
    private LocalDateTime bookingTime;

    @ManyToOne
    @JoinColumn(name = "staff_id")
    private User staff;

    @Column(nullable = false)
    private String address;

    private String phone;

    @Column(length = 500)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.NEW;

    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean isCancellable() {
        if (status != BookingStatus.NEW && status != BookingStatus.CONFIRMED) return false;
        if (largeOrder && bookingTime != null) {
            return bookingTime.isAfter(LocalDateTime.now().plusMinutes(60));
        }
        return true;
    }

    public String getSummary() {
        return items.stream()
                .map(i -> i.getDrink().getName() + " x " + i.getQuantity())
                .collect(Collectors.joining(", "));
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getCustomer() { return customer; }
    public void setCustomer(User customer) { this.customer = customer; }
    public List<BookingItem> getItems() { return items; }
    public void setItems(List<BookingItem> items) { this.items = items; }
    public int getTotalCups() { return totalCups; }
    public void setTotalCups(int totalCups) { this.totalCups = totalCups; }
    public Long getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Long totalPrice) { this.totalPrice = totalPrice; }
    public boolean isLargeOrder() { return largeOrder; }
    public void setLargeOrder(boolean largeOrder) { this.largeOrder = largeOrder; }
    public LocalDateTime getBookingTime() { return bookingTime; }
    public void setBookingTime(LocalDateTime bookingTime) { this.bookingTime = bookingTime; }
    public User getStaff() { return staff; }
    public void setStaff(User staff) { this.staff = staff; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}