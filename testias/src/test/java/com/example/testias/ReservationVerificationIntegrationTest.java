package com.example.testias;

import com.example.testias.api.ApiDtos.ReservationRequest;
import com.example.testias.api.ApiDtos.ReservationResponse;
import com.example.testias.domain.Availability;
import com.example.testias.domain.ReservationStatus;
import com.example.testias.domain.Vehicle;
import com.example.testias.repository.ReservationRepository;
import com.example.testias.repository.VehicleRepository;
import com.example.testias.service.ReservationService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ReservationVerificationIntegrationTest {
    @Autowired ReservationService service;
    @Autowired VehicleRepository vehicles;
    @Autowired ReservationRepository reservations;
    @Autowired EntityManager entityManager;

    private final List<String> vehicleIds = new ArrayList<>();
    private final List<String> references = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        references.forEach(reference -> reservations.findByReferenceId(reference).ifPresent(reservations::delete));
        vehicleIds.forEach(id -> vehicles.findById(id).ifPresent(vehicles::delete));
        references.clear();
        vehicleIds.clear();
    }

    @Test
    void availableVehicleBecomesReservedAfterConfirmation() {
        String vehicleId = vehicle("AVAILABLE");
        String reference = reference("CONFIRM");

        ReservationResponse result = service.create(new ReservationRequest(reference, vehicleId, "BUYER-1"));

        assertThat(result.status()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(vehicles.findById(vehicleId).orElseThrow().getAvailability()).isEqualTo(Availability.RESERVED);
    }

    @Test
    void confirmedAndRejectedResultsReplayForIdenticalRequests() {
        String confirmedVehicle = vehicle("REPLAY-CONF");
        String confirmedReference = reference("REPLAY-CONF");
        ReservationRequest confirmedRequest = new ReservationRequest(confirmedReference, confirmedVehicle, "BUYER-1");
        ReservationResponse confirmed = service.create(confirmedRequest);
        ReservationResponse confirmedReplay = service.create(confirmedRequest);

        String rejectedVehicle = vehicle("REPLAY-REJ");
        String rejectedReference = reference("REPLAY-REJ");
        service.create(new ReservationRequest(reference("BLOCK"), rejectedVehicle, "BUYER-BLOCK"));
        ReservationRequest rejectedRequest = new ReservationRequest(rejectedReference, rejectedVehicle, "BUYER-2");
        ReservationResponse rejected = service.create(rejectedRequest);
        ReservationResponse rejectedReplay = service.create(rejectedRequest);

        assertThat(confirmed.status()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(confirmedReplay.status()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(rejected.status()).isEqualTo(ReservationStatus.REJECTED);
        assertThat(rejectedReplay.status()).isEqualTo(ReservationStatus.REJECTED);
        assertThat(reservations.findByReferenceId(confirmedReference)).isPresent();
        assertThat(reservations.findByReferenceId(rejectedReference)).isPresent();
    }

    @Test
    void differentBusinessDataForExistingReferenceIsConflict() {
        String vehicleId = vehicle("CONFLICT");
        String reference = reference("CONFLICT");
        service.create(new ReservationRequest(reference, vehicleId, "BUYER-1"));

        assertThatThrownBy(() -> service.create(new ReservationRequest(reference, vehicleId, "BUYER-2")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("different business data");
        assertThat(reservations.findByReferenceId(reference).orElseThrow().getBuyerId()).isEqualTo("BUYER-1");
    }

    @Test
    void unknownReferenceAndRejectedReferenceAreDistinguishable() {
        assertThatThrownBy(() -> service.find(reference("UNKNOWN")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("does not exist");

        String vehicleId = vehicle("LOOKUP-REJECT");
        service.create(new ReservationRequest(reference("BLOCK-LOOKUP"), vehicleId, "BUYER-BLOCK"));
        String rejectedReference = reference("LOOKUP-REJECT");
        ReservationResponse rejected = service.create(new ReservationRequest(rejectedReference, vehicleId, "BUYER-2"));

        ReservationResponse lookup = service.find(rejectedReference);
        assertThat(rejected.status()).isEqualTo(ReservationStatus.REJECTED);
        assertThat(lookup.status()).isEqualTo(ReservationStatus.REJECTED);
        assertThat(lookup.reason()).isNotBlank();
    }

    @Test
    void reservationAndVehicleStateRemainAfterPersistenceContextIsCleared() {
        String vehicleId = vehicle("PERSIST");
        String reference = reference("PERSIST");
        ReservationResponse created = service.create(new ReservationRequest(reference, vehicleId, "BUYER-1"));

        entityManager.clear();

        ReservationResponse restored = service.find(reference);
        assertThat(restored.status()).isEqualTo(created.status());
        assertThat(restored.vehicle().availability()).isEqualTo(Availability.RESERVED);
        assertThat(vehicles.findById(vehicleId).orElseThrow().getAvailability()).isEqualTo(Availability.RESERVED);
    }

    private String vehicle(String suffix) {
        String id = "VERIFY-" + suffix + "-" + UUID.randomUUID().toString().substring(0, 8);
        vehicles.save(new Vehicle(id, "Test", suffix, 2026, BigDecimal.TEN));
        vehicleIds.add(id);
        return id;
    }

    private String reference(String suffix) {
        String reference = "VERIFY-" + suffix + "-" + UUID.randomUUID().toString().substring(0, 8);
        references.add(reference);
        return reference;
    }
}
