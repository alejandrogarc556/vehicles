package com.example.testias;

import com.example.testias.api.ApiDtos.ReservationRequest;
import com.example.testias.api.ApiDtos.ReservationResponse;
import com.example.testias.domain.ReservationStatus;
import com.example.testias.domain.Vehicle;
import com.example.testias.repository.ReservationRepository;
import com.example.testias.repository.VehicleRepository;
import com.example.testias.service.ReservationService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ReservationConcurrencyIntegrationTest {
    private static final String VEHICLE_ID = "TEST-CONCURRENCY-001";
    private static final String REF_A = "TEST-CONCURRENCY-A";
    private static final String REF_B = "TEST-CONCURRENCY-B";

    @Autowired ReservationService service;
    @Autowired VehicleRepository vehicles;
    @Autowired ReservationRepository reservations;

    @BeforeEach
    void setUp() {
        reservations.findByReferenceId(REF_A).ifPresent(r -> reservations.delete(r));
        reservations.findByReferenceId(REF_B).ifPresent(r -> reservations.delete(r));
        vehicles.deleteById(VEHICLE_ID);
        vehicles.save(new Vehicle(VEHICLE_ID, "Test", "Concurrency", 2026, BigDecimal.TEN));
    }

    @AfterEach
    void tearDown() {
        reservations.findByReferenceId(REF_A).ifPresent(r -> reservations.delete(r));
        reservations.findByReferenceId(REF_B).ifPresent(r -> reservations.delete(r));
        vehicles.deleteById(VEHICLE_ID);
    }

    @Test
    void concurrentRequestsProduceOneConfirmationAndOneRejection() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Callable<ReservationResponse> callA = () -> submit(ready, start, REF_A, "BUY-A");
            Callable<ReservationResponse> callB = () -> submit(ready, start, REF_B, "BUY-B");
            Future<ReservationResponse> a = executor.submit(callA);
            Future<ReservationResponse> b = executor.submit(callB);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<ReservationStatus> statuses = List.of(a.get(10, TimeUnit.SECONDS).status(), b.get(10, TimeUnit.SECONDS).status());
            assertThat(statuses).containsExactlyInAnyOrder(ReservationStatus.CONFIRMED, ReservationStatus.REJECTED);
        } finally {
            executor.shutdownNow();
        }
    }

    private ReservationResponse submit(CountDownLatch ready, CountDownLatch start, String reference, String buyer) throws Exception {
        ready.countDown();
        assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
        return service.create(new ReservationRequest(reference, VEHICLE_ID, buyer));
    }
}
