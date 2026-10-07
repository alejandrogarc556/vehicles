package com.example.testias.api;

import com.example.testias.domain.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public final class ApiDtos {
    private ApiDtos() {
    }

    public record ReservationRequest(@NotBlank @Size(max = 50) String referenceId,
                                     @NotBlank @Size(max = 50) String vehicleId,
                                     @NotBlank @Size(max = 50) String buyerId) {
    }

    public record VehicleResponse(String id, String brand, String model, Integer year, BigDecimal price,
                                  Availability availability) {
        public static VehicleResponse from(Vehicle v) {
            return new VehicleResponse(v.getId(), v.getBrand(), v.getModel(), v.getYear(), v.getPrice(), v.getAvailability());
        }
    }

    public record ReservationResponse(String referenceId, VehicleResponse vehicle, String buyerId,
                                      ReservationStatus status, String reason, OffsetDateTime createdAt) {
        public static ReservationResponse from(Reservation r, Vehicle v) {
            return new ReservationResponse(r.getReferenceId(), v == null ? null : VehicleResponse.from(v), r.getBuyerId(), r.getStatus(), r.getReason(), r.getCreatedAt());
        }
    }

    public record ErrorResponse(String code, String message) {
    }
}
