package com.example.testias.service;

import com.example.testias.api.ApiDtos.*;
import com.example.testias.domain.*;
import com.example.testias.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {
    @Mock ReservationRepository reservations;
    @Mock VehicleRepository vehicles;
    @Mock ReservationTransactionService transactionService;
    @InjectMocks ReservationService service;

    @Test
    void identicalReferenceReturnsOriginalWithoutProcessingAgain() {
        Reservation original = new Reservation("REF-001", "VM-001", "BUY-001", ReservationStatus.REJECTED, "Vehicle is not available");
        Vehicle vehicle = new Vehicle("VM-001", "Toyota", "Corolla", 2024, BigDecimal.valueOf(24500));
        when(reservations.findByReferenceId("REF-001")).thenReturn(Optional.of(original));
        when(vehicles.findById("VM-001")).thenReturn(Optional.of(vehicle));

        ReservationResponse result = service.create(new ReservationRequest("REF-001", "VM-001", "BUY-001"));

        assertThat(result.status()).isEqualTo(ReservationStatus.REJECTED);
        verifyNoInteractions(transactionService);
    }

    @Test
    void differentDataForExistingReferenceIsConflict() {
        when(reservations.findByReferenceId("REF-001")).thenReturn(Optional.of(
                new Reservation("REF-001", "VM-001", "BUY-001", ReservationStatus.CONFIRMED, null)));

        assertThatThrownBy(() -> service.create(new ReservationRequest("REF-001", "VM-002", "BUY-001")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("different business data");
        verifyNoInteractions(transactionService);
    }
}
