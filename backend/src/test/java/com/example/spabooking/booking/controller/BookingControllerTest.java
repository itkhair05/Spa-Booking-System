package com.example.spabooking.booking.controller;

import com.example.spabooking.auth.entity.User;
import com.example.spabooking.auth.enums.UserRole;
import com.example.spabooking.auth.repository.UserRepository;
import com.example.spabooking.auth.security.CustomUserDetails;
import com.example.spabooking.auth.security.JwtUtils;
import com.example.spabooking.booking.entity.Booking;
import com.example.spabooking.booking.enums.BookingStatus;
import com.example.spabooking.booking.repository.BookingRepository;
import com.example.spabooking.customer.entity.Customer;
import com.example.spabooking.customer.repository.CustomerRepository;
import com.example.spabooking.service.repository.ServiceRepository;
import com.example.spabooking.staff.entity.Staff;
import com.example.spabooking.staff.repository.StaffRepository;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class BookingControllerTest {

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
    private JwtUtils jwtUtils;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    private String ownerJwt;
    private String staffJwt;
    private String ownerTenantBJwt;

    private Tenant tenantA;
    private Tenant tenantB;
    
    private Customer customerA;
    private Staff staffA;
    private com.example.spabooking.service.entity.Service serviceA;

    private Customer customerB;
    private Staff staffB;
    private com.example.spabooking.service.entity.Service serviceB;
    
    private Booking bookingA;

    @BeforeEach
    void setUp() {
        objectMapper.findAndRegisterModules();

        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        tenantA = new Tenant();
        tenantA.setName("Tenant A");
        tenantA.setSlug("tenant-a-booking");
        tenantA = tenantRepository.saveAndFlush(tenantA);

        tenantB = new Tenant();
        tenantB.setName("Tenant B");
        tenantB.setSlug("tenant-b-booking");
        tenantB = tenantRepository.saveAndFlush(tenantB);

        User ownerA = new User();
        ownerA.setUsername("ownerABook");
        ownerA.setPassword("encoded");
        ownerA.setRole(UserRole.OWNER);
        ownerA.setTenant(tenantA);
        ownerA = userRepository.saveAndFlush(ownerA);

        User staffUserA = new User();
        staffUserA.setUsername("staffABook");
        staffUserA.setPassword("encoded");
        staffUserA.setRole(UserRole.STAFF);
        staffUserA.setTenant(tenantA);
        staffUserA = userRepository.saveAndFlush(staffUserA);

        User ownerB = new User();
        ownerB.setUsername("ownerBBook");
        ownerB.setPassword("encoded");
        ownerB.setRole(UserRole.OWNER);
        ownerB.setTenant(tenantB);
        ownerB = userRepository.saveAndFlush(ownerB);

        ownerJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(new CustomUserDetails(ownerA), null, new CustomUserDetails(ownerA).getAuthorities()));
        staffJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(new CustomUserDetails(staffUserA), null, new CustomUserDetails(staffUserA).getAuthorities()));
        ownerTenantBJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(new CustomUserDetails(ownerB), null, new CustomUserDetails(ownerB).getAuthorities()));

        customerA = new Customer();
        customerA.setTenant(tenantA);
        customerA.setName("Alice");
        customerA.setIsActive(true);
        customerA = customerRepository.saveAndFlush(customerA);

        staffA = new Staff();
        staffA.setTenant(tenantA);
        staffA.setName("Staff A");
        staffA.setIsActive(true);
        staffA = staffRepository.saveAndFlush(staffA);

        // STAFF identity must come from the user-staff link; bookings are scoped to it
        staffUserA.setStaff(staffA);
        userRepository.saveAndFlush(staffUserA);

        serviceA = new com.example.spabooking.service.entity.Service();
        serviceA.setTenant(tenantA);
        serviceA.setName("Massage");
        serviceA.setPrice(new BigDecimal("300000.00"));
        serviceA.setDurationMinutes(60);
        serviceA.setIsActive(true);
        serviceA = serviceRepository.saveAndFlush(serviceA);

        customerB = new Customer();
        customerB.setTenant(tenantB);
        customerB.setName("Bob");
        customerB.setIsActive(true);
        customerB = customerRepository.saveAndFlush(customerB);

        staffB = new Staff();
        staffB.setTenant(tenantB);
        staffB.setName("Staff B");
        staffB.setIsActive(true);
        staffB = staffRepository.saveAndFlush(staffB);

        serviceB = new com.example.spabooking.service.entity.Service();
        serviceB.setTenant(tenantB);
        serviceB.setName("Haircut");
        serviceB.setPrice(new BigDecimal("150000.00"));
        serviceB.setDurationMinutes(30);
        serviceB.setIsActive(true);
        serviceB = serviceRepository.saveAndFlush(serviceB);

        bookingA = new Booking();
        bookingA.setTenant(tenantA);
        bookingA.setCustomer(customerA);
        bookingA.setStaff(staffA);
        bookingA.setService(serviceA);
        bookingA.setStartTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0));
        bookingA.setEndTime(bookingA.getStartTime().plusMinutes(60));
        bookingA.setStatus(BookingStatus.PENDING);
        bookingA.setPrice(serviceA.getPrice());
        bookingA = bookingRepository.saveAndFlush(bookingA);
    }

    @Test
    void testOwnerGetBookings() throws Exception {
        mockMvc.perform(get("/api/v1/bookings")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(bookingA.getId().intValue())));
    }

    @Test
    void testCrossTenantGet() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/" + bookingA.getId())
                        .header("Authorization", "Bearer " + ownerTenantBJwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateBooking() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(14).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusMinutes(60);
        String requestJson = """
                {
                    "customerId": %d,
                    "serviceId": %d,
                    "staffId": %d,
                    "startTime": "%s",
                    "endTime": "%s"
                }
                """.formatted(
                        customerA.getId(),
                        serviceA.getId(),
                        staffA.getId(),
                        start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        end.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + staffJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.price", is(300000.0)));
    }

    @Test
    void testCreateBookingOverlapConflictStaff() throws Exception {
        // Try to book overlapping time for same staff
        LocalDateTime start = bookingA.getStartTime().plusMinutes(30); // 10:30
        LocalDateTime end = start.plusMinutes(60); // 11:30
        String requestJson = """
                {
                    "customerId": %d,
                    "serviceId": %d,
                    "staffId": %d,
                    "startTime": "%s",
                    "endTime": "%s"
                }
                """.formatted(
                customerB.getId(), // Different customer just in case, but they are tenant B so it will fail 404. Let's use customerA (or a new customer A2).
                serviceA.getId(),
                staffA.getId(),
                start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                end.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        Customer customerA2 = new Customer();
        customerA2.setTenant(tenantA);
        customerA2.setName("Alice 2");
        customerA2.setIsActive(true);
        customerA2 = customerRepository.saveAndFlush(customerA2);

        requestJson = requestJson.replace(String.valueOf(customerB.getId()), String.valueOf(customerA2.getId()));

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isConflict()); // 409 Conflict
    }

    @Test
    void testUpdateBookingStatus() throws Exception {
        String requestJson = """
                {
                    "status": "CONFIRMED"
                }
                """;

        mockMvc.perform(patch("/api/v1/bookings/" + bookingA.getId() + "/status")
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFIRMED")));
    }
}
