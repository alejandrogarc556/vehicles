package com.example.testias.api;

import com.example.testias.api.ApiDtos.*;
import com.example.testias.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {
    private final ReservationService service;

    public ReservationController(ReservationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @GetMapping("/{referenceId}")
    public ReservationResponse find(@PathVariable String referenceId) {
        return service.find(referenceId);
    }
}
