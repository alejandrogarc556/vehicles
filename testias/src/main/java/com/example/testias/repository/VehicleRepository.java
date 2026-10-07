package com.example.testias.repository;

import com.example.testias.domain.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, String> {
    Page<Vehicle> findByBrandIgnoreCase(String brand, Pageable pageable);
}
