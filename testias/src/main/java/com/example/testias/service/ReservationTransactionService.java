package com.example.testias.service;

import com.example.testias.api.ApiDtos.*;
import com.example.testias.domain.*;
import com.example.testias.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReservationTransactionService {
    private final ReservationRepository reservations;
    private final VehicleRepository vehicles;

    public ReservationTransactionService(ReservationRepository reservations, VehicleRepository vehicles) {
        this.reservations = reservations;
        this.vehicles = vehicles;
    }

    @Transactional
    public ReservationResponse process(ReservationRequest request) {
        Vehicle vehicle = vehicles.findById(request.vehicleId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle does not exist"));
        if (vehicle.getAvailability() != Availability.AVAILABLE) {
            return saveResult(new Reservation(request.referenceId(), request.vehicleId(), request.buyerId(),
                    ReservationStatus.REJECTED, "Vehicle is not available"), vehicle);
        }
        vehicle.reserve();
        vehicles.saveAndFlush(vehicle);
        return saveResult(new Reservation(request.referenceId(), request.vehicleId(), request.buyerId(),
                ReservationStatus.CONFIRMED, null), vehicle);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReservationResponse persistRejectedAfterConcurrency(ReservationRequest request) {
        Reservation existing = reservations.findByReferenceId(request.referenceId()).orElse(null);
        if (existing != null)
            return ReservationResponse.from(existing, vehicles.findById(existing.getVehicleId()).orElse(null));
        Vehicle vehicle = vehicles.findById(request.vehicleId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle does not exist"));
        return saveResult(new Reservation(request.referenceId(), request.vehicleId(), request.buyerId(),
                ReservationStatus.REJECTED, "Vehicle was reserved by another request"), vehicle);
    }

    private ReservationResponse saveResult(Reservation reservation, Vehicle vehicle) {
        return ReservationResponse.from(reservations.saveAndFlush(reservation), vehicle);
    }
}
