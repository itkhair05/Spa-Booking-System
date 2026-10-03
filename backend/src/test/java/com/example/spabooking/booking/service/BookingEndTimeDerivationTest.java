package com.example.spabooking.booking.service;

import com.example.spabooking.auth.entity.User;
import com.example.spabooking.auth.enums.UserRole;
import com.example.spabooking.auth.repository.UserRepository;
import com.example.spabooking.auth.security.CustomUserDetails;
import com.example.spabooking.auth.security.JwtUtils;
import com.example.spabooking.booking.dto.CreateBookingRequest;
import com.example.spabooking.booking.dto.UpdateBookingRequest;
import com.example.spabooking.booking.entity.Booking;
import com.example.spabooking.booking.enums.BookingStatus;
import com.example.spabooking.booking.exception.BookingConflictException;
import com.example.spabooking.booking.repository.BookingRepository;
import com.example.spabooking.customer.entity.Customer;
import com.example.spabooking.customer.repository.CustomerRepository;
import com.example.spabooking.service.repository.ServiceRepository;
import com.example.spabooking.staff.entity.Staff;
import com.example.spabooking.staff.repository.StaffRepository;
import com.example.spabooking.tenant.context.TenantContext;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class BookingEndTimeDerivationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

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

    @Autowired
    private BookingService bookingService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private EntityManager entityManager;

    private MockMvc mockMvc;
    private String ownerJwt;

    private Tenant tenant;
    private Customer customer;
    private Staff staff;
    private com.example.spabooking.service.entity.Service service60;
    private com.example.spabooking.service.entity.Service service90;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        tenant = new Tenant();
        tenant.setName("EndTime Derivation Spa");
        tenant.setSlug("endtime-derivation-" + System.nanoTime());
        tenant.setIsActive(true);
        tenant = tenantRepository.saveAndFlush(tenant);

        User owner = new User();
        owner.setUsername("ownerEndTime" + System.nanoTime());
        owner.setPassword("encoded");
        owner.setRole(UserRole.OWNER);
        owner.setTenant(tenant);
        owner = userRepository.saveAndFlush(owner);
        ownerJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(owner), null, new CustomUserDetails(owner).getAuthorities()));

        customer = new Customer();
        customer.setTenant(tenant);
        customer.setName("Alice");
        customer.setIsActive(true);
        customer = customerRepository.saveAndFlush(customer);

        staff = new Staff();
        staff.setTenant(tenant);
        staff.setName("Staff One");
        staff.setIsActive(true);
        staff = staffRepository.saveAndFlush(staff);

        service60 = new com.example.spabooking.service.entity.Service();
        service60.setTenant(tenant);
        service60.setName("Facial Basic");
        service60.setDurationMinutes(60);
        service60.setPrice(new BigDecimal("300000.00"));
        service60.setIsActive(true);
        service60 = serviceRepository.saveAndFlush(service60);

        service90 = new com.example.spabooking.service.entity.Service();
        service90.setTenant(tenant);
        service90.setName("Deep Cleansing Facial");
        service90.setDurationMinutes(90);
        service90.setPrice(new BigDecimal("450000.00"));
        service90.setIsActive(true);
        service90 = serviceRepository.saveAndFlush(service90);

        TenantContext.setTenantId(tenant.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createDerivesEndTimeFromServiceDurationIgnoringClientValue() {
        LocalDateTime start = futureAt(2, 13, 0);
        LocalDateTime maliciousClientEnd = start.plusHours(2);

        Booking created = bookingService.create(
                newCreateRequest(customer.getId(), service60.getId(), start, maliciousClientEnd));

        Booking persisted = reload(created.getId());
        assertEquals(start, persisted.getStartTime());
        assertEquals(start.plusMinutes(60), persisted.getEndTime());
        assertEquals(service60.getId(), persisted.getService().getId());
        assertEquals(staff.getId(), persisted.getStaff().getId());
        assertEquals(tenant.getId(), persisted.getTenant().getId());
    }

    @Test
    void rescheduleDerivesEndTimeFromNewStart() {
        Booking seeded = seedBooking(futureAt(2, 10, 0), futureAt(2, 11, 0), service60);

        LocalDateTime newStart = futureAt(2, 15, 0);
        UpdateBookingRequest request = new UpdateBookingRequest();
        request.setCustomerId(customer.getId());
        request.setServiceId(service60.getId());
        request.setStaffId(staff.getId());
        request.setStartTime(newStart);
        request.setEndTime(newStart.plusHours(5));

        bookingService.update(seeded.getId(), request);

        Booking persisted = reload(seeded.getId());
        assertEquals(newStart, persisted.getStartTime());
        assertEquals(newStart.plusMinutes(60), persisted.getEndTime());
    }

    @Test
    void serviceChangeRecalculatesEndTimeWithNewDurationAndKeepsPriceSnapshot() {
        Booking seeded = seedBooking(futureAt(2, 10, 0), futureAt(2, 11, 0), service60);

        LocalDateTime newStart = futureAt(2, 15, 0);
        UpdateBookingRequest request = new UpdateBookingRequest();
        request.setCustomerId(customer.getId());
        request.setServiceId(service90.getId());
        request.setStaffId(staff.getId());
        request.setStartTime(newStart);
        request.setEndTime(newStart.plusHours(5));

        bookingService.update(seeded.getId(), request);

        Booking persisted = reload(seeded.getId());
        assertEquals(newStart, persisted.getStartTime());
        assertEquals(newStart.plusMinutes(90), persisted.getEndTime());
        assertEquals(service90.getId(), persisted.getService().getId());
        assertEquals(0, new BigDecimal("300000.00").compareTo(persisted.getPrice()));
    }

    @Test
    void overlapDetectionUsesDerivedEndTimeNotClientSuppliedEndTime() {
        LocalDateTime existingStart = futureAt(2, 13, 30);
        seedBooking(existingStart, existingStart.plusMinutes(60), service60);

        // Client claims 13:00-13:15 (would not overlap). Derived end is 14:00, which does overlap 13:30-14:30.
        LocalDateTime start = futureAt(2, 13, 0);
        CreateBookingRequest request = newCreateRequest(
                customer.getId(), service60.getId(), start, start.plusMinutes(15));

        assertThrows(BookingConflictException.class, () -> bookingService.create(request));

        entityManager.flush();
        entityManager.clear();
        assertEquals(1, bookingRepository.findAllByTenantId(tenant.getId()).size());
    }

    @Test
    void wallClockLocalDateTimeIsPersistedUnchanged() {
        LocalDateTime start = LocalDateTime.of(2026, 12, 12, 10, 0);

        Booking created = bookingService.create(
                newCreateRequest(customer.getId(), service60.getId(), start, start.plusHours(2)));

        Booking persisted = reload(created.getId());
        assertEquals(LocalDateTime.of(2026, 12, 12, 10, 0), persisted.getStartTime());
        assertEquals(LocalDateTime.of(2026, 12, 12, 11, 0), persisted.getEndTime());
    }

    @Test
    void wallClockWireFormatKeepsLocalTimeThroughHttpPipeline() throws Exception {
        LocalDateTime start = futureAt(3, 10, 0);
        String startText = start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String clientEndText = start.plusHours(2).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String requestJson = """
                {
                    "customerId": %d,
                    "serviceId": %d,
                    "staffId": %d,
                    "startTime": "%s",
                    "endTime": "%s"
                }
                """.formatted(customer.getId(), service60.getId(), staff.getId(), startText, clientEndText);

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());

        entityManager.flush();
        entityManager.clear();
        List<Booking> bookings = bookingRepository.findAllByTenantId(tenant.getId());
        assertEquals(1, bookings.size());
        assertEquals(start, bookings.get(0).getStartTime());
        assertEquals(start.plusMinutes(60), bookings.get(0).getEndTime());
    }

    private LocalDateTime futureAt(int daysFromNow, int hour, int minute) {
        return LocalDateTime.now().plusDays(daysFromNow)
                .withHour(hour).withMinute(minute).withSecond(0).withNano(0);
    }

    private CreateBookingRequest newCreateRequest(Long customerId, Long serviceId,
                                                  LocalDateTime start, LocalDateTime clientEndTime) {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setCustomerId(customerId);
        request.setServiceId(serviceId);
        request.setStaffId(staff.getId());
        request.setStartTime(start);
        request.setEndTime(clientEndTime);
        return request;
    }

    private Booking seedBooking(LocalDateTime start, LocalDateTime end,
                                com.example.spabooking.service.entity.Service service) {
        Booking booking = new Booking();
        booking.setTenant(tenant);
        booking.setCustomer(customer);
        booking.setStaff(staff);
        booking.setService(service);
        booking.setStartTime(start);
        booking.setEndTime(end);
        booking.setStatus(BookingStatus.PENDING);
        booking.setPrice(service.getPrice());
        return bookingRepository.saveAndFlush(booking);
    }

    private Booking reload(Long id) {
        entityManager.flush();
        entityManager.clear();
        return bookingRepository.findByIdAndTenantId(id, tenant.getId()).orElseThrow();
    }
}
