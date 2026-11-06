package com.drinknbooking.drinkbooking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "drink_images")
public class DrinkImage {
    @Id
    private Long drinkId;

    @Column(columnDefinition = "LONGBLOB")
    private byte[] data;

    public Long getDrinkId() { return drinkId; }
    public void setDrinkId(Long drinkId) { this.drinkId = drinkId; }
    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }
}