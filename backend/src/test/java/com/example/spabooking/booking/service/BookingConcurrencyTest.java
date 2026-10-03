package com.example.spabooking.booking.service;

import com.example.spabooking.booking.dto.CreateBookingRequest;
import com.example.spabooking.booking.exception.BookingConflictException;
import com.example.spabooking.booking.repository.BookingRepository;
import com.example.spabooking.customer.entity.Customer;
import com.example.spabooking.customer.repository.CustomerRepository;
import com.example.spabooking.service.entity.Service;
import com.example.spabooking.service.repository.ServiceRepository;
import com.example.spabooking.staff.entity.Staff;
import com.example.spabooking.staff.repository.StaffRepository;
import com.example.spabooking.tenant.context.TenantContext;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class BookingConcurrencyTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private BookingRepository bookingRepository;

    private Tenant tenant;
    private Customer customer1;
    private Customer customer2;
    private Staff staff;
    private Service service;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setName("Concurrency Test Spa");
        tenant.setSlug("concurrency-test-" + System.nanoTime());
        tenant.setIsActive(true);
        tenant = tenantRepository.saveAndFlush(tenant);

        staff = new Staff();
        staff.setTenant(tenant);
        staff.setName("Concurrency Staff");
        staff.setIsActive(true);
        staff = staffRepository.saveAndFlush(staff);

        service = new Service();
        service.setTenant(tenant);
        service.setName("Concurrency Service");
        service.setDurationMinutes(60);
        service.setPrice(new BigDecimal("100000.00"));
        service.setIsActive(true);
        service = serviceRepository.saveAndFlush(service);

        Customer c1 = new Customer();
        c1.setTenant(tenant);
        c1.setName("Concurrency Customer 1");
        c1.setIsActive(true);
        customer1 = customerRepository.saveAndFlush(c1);

        Customer c2 = new Customer();
        c2.setTenant(tenant);
        c2.setName("Concurrency Customer 2");
        c2.setIsActive(true);
        customer2 = customerRepository.saveAndFlush(c2);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        try {
            bookingRepository.findAllByTenantId(tenant.getId()).forEach(bookingRepository::delete);
            bookingRepository.flush();
            customerRepository.delete(customer1);
            customerRepository.delete(customer2);
            staffRepository.delete(staff);
            serviceRepository.delete(service);
            customerRepository.flush();
            staffRepository.flush();
            serviceRepository.flush();
            tenantRepository.delete(tenant);
            tenantRepository.flush();
        } catch (Exception ignored) {
            // best-effort cleanup: the test must not fail because of teardown
        }
    }

    @Test
    @Timeout(60)
    void concurrentOverlappingBookingsForSameStaffOnlyOneSucceeds() throws Exception {
        LocalDateTime day = LocalDateTime.now().plusDays(1)
                .withHour(10).withMinute(0).withSecond(0).withNano(0);

        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<String> f1 = executor.submit(
                    attempt(customer1.getId(), day, day.plusHours(1), barrier));
            Future<String> f2 = executor.submit(
                    attempt(customer2.getId(), day.plusMinutes(30), day.plusMinutes(90), barrier));

            List<String> results = List.of(
                    f1.get(30, TimeUnit.SECONDS),
                    f2.get(30, TimeUnit.SECONDS));

            assertEquals(1, results.stream().filter("SUCCESS"::equals).count(),
                    "Exactly one overlapping booking may succeed, got: " + results);
            assertEquals(1, results.stream().filter("CONFLICT"::equals).count(),
                    "The competing booking must be rejected with a conflict, got: " + results);

            long persisted = bookingRepository.findAllByTenantId(tenant.getId()).size();
            assertTrue(persisted == 1,
                    "Exactly one booking row may be persisted, but found " + persisted);
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<String> attempt(Long customerId, LocalDateTime start, LocalDateTime end, CyclicBarrier barrier) {
        return () -> {
            TenantContext.setTenantId(tenant.getId());
            try {
                barrier.await(10, TimeUnit.SECONDS);

                CreateBookingRequest request = new CreateBookingRequest();
                request.setCustomerId(customerId);
                request.setStaffId(staff.getId());
                request.setServiceId(service.getId());
                request.setStartTime(start);
                request.setEndTime(end);

                bookingService.create(request);
                return "SUCCESS";
            } catch (BookingConflictException e) {
                return "CONFLICT";
            } finally {
                TenantContext.clear();
            }
        };
    }
}
