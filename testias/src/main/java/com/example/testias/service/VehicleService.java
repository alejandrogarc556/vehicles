package com.example.testias.service;

import com.example.testias.api.ApiDtos.VehicleResponse;
import com.example.testias.repository.VehicleRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
public class VehicleService {
    private final VehicleRepository repository;

    public VehicleService(VehicleRepository repository) {
        this.repository = repository;
    }

    public Page<VehicleResponse> search(String brand, int page, int size) {
        if (page < 0) throw new IllegalArgumentException("page must be zero or greater");
        if (size < 1 || size > 100) throw new IllegalArgumentException("size must be between 1 and 100");
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        return (brand == null || brand.isBlank() ? repository.findAll(pageable) : repository.findByBrandIgnoreCase(brand.trim(), pageable)).map(VehicleResponse::from);
    }
}
