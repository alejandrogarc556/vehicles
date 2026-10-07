package com.example.testias.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "vehicles")
public class Vehicle {
    @Id
    private String id;
    @Column(nullable = false)
    private String brand;
    @Column(nullable = false)
    private String model;
    @Column(nullable = false)
    private Integer year;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal price;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Availability availability;
    @Version
    @Column(nullable = false)
    private Long version;

    protected Vehicle() {
    }

    public Vehicle(String id, String brand, String model, Integer year, BigDecimal price) {
        this.id = id;
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.price = price;
        this.availability = Availability.AVAILABLE;
    }

    public void reserve() {
        this.availability = Availability.RESERVED;
    }

    public String getId() {
        return id;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public Integer getYear() {
        return year;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Availability getAvailability() {
        return availability;
    }

    public Long getVersion() {
        return version;
    }
}
