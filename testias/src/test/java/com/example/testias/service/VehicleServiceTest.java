package com.example.testias.service;

import com.example.testias.domain.Vehicle;
import com.example.testias.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {
    @Mock VehicleRepository repository;
    @InjectMocks VehicleService service;

    @Test
    void returnsStablePagedCatalog() {
        Vehicle vehicle = new Vehicle("VM-001", "Toyota", "Corolla", 2024, BigDecimal.valueOf(24500));
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(vehicle), PageRequest.of(1, 1), 2));

        Page<?> result = service.search(null, 1, 1);

        assertThat(result.getContent()).hasSize(1);
        verify(repository).findAll(argThat((Pageable p) -> p.getPageNumber() == 1 && p.getPageSize() == 1
                && p.getSort().getOrderFor("id") != null));

    }
}
