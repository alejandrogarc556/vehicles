package com.example.testias.service;

import com.example.testias.api.ApiDtos.*;
import com.example.testias.domain.*;
import com.example.testias.repository.*;
import jakarta.persistence.OptimisticLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReservationService {
    private final ReservationRepository reservations;
    private final VehicleRepository vehicles;
    private final ReservationTransactionService transactionService;

    public ReservationService(ReservationRepository reservations, VehicleRepository vehicles,
                              ReservationTransactionService transactionService) {
        this.reservations = reservations;
        this.vehicles = vehicles;
        this.transactionService = transactionService;
    }

    public ReservationResponse create(ReservationRequest request) {
        Reservation existing = reservations.findByReferenceId(request.referenceId()).orElse(null);
        if (existing != null) return replayOrConflict(existing, request);
        try {
            return transactionService.process(request);
        } catch (DataIntegrityViolationException ex) {
            Reservation concurrent = reservations.findByReferenceId(request.referenceId()).orElse(null);
            if (concurrent != null) return replayOrConflict(concurrent, request);
            throw ex;
        } catch (OptimisticLockingFailureException | OptimisticLockException ex) {
            ReservationResponse result = transactionService.persistRejectedAfterConcurrency(request);
            return replayOrConflict(result, request);
        }
    }

    @Transactional(readOnly = true)
    public ReservationResponse find(String referenceId) {
        Reservation reservation = reservations.findByReferenceId(referenceId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation reference does not exist"));
        return ReservationResponse.from(reservation, vehicles.findById(reservation.getVehicleId()).orElse(null));
    }

    private ReservationResponse replayOrConflict(Reservation existing, ReservationRequest request) {
        if (!existing.getVehicleId().equals(request.vehicleId()) || !existing.getBuyerId().equals(request.buyerId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reference already exists with different business data");
        }
        return ReservationResponse.from(existing, vehicles.findById(existing.getVehicleId()).orElse(null));
    }

    private ReservationResponse replayOrConflict(ReservationResponse existing, ReservationRequest request) {
        if (existing.vehicle() == null || !existing.vehicle().id().equals(request.vehicleId())
                || !existing.buyerId().equals(request.buyerId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reference already exists with different business data");
        }
        return existing;
    }
}
