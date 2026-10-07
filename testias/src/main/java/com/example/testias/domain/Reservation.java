package com.example.testias.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "reservations")
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "reference_id", nullable = false, unique = true, length = 50)
    private String referenceId;
    @Column(name = "vehicle_id", nullable = false, length = 50)
    private String vehicleId;
    @Column(name = "buyer_id", nullable = false, length = 50)
    private String buyerId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;
    @Column(length = 255)
    private String reason;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Reservation() {
    }

    public Reservation(String referenceId, String vehicleId, String buyerId,
                       ReservationStatus status, String reason) {
        this.referenceId = referenceId;
        this.vehicleId = vehicleId;
        this.buyerId = buyerId;
        this.status = status;
        this.reason = reason;
        this.createdAt = OffsetDateTime.now();
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public String getBuyerId() {
        return buyerId;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
