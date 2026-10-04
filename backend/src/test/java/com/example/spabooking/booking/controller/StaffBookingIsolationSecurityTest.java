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
import com.example.spabooking.service.entity.Service;
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
public class StaffBookingIsolationSecurityTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private JwtUtils jwtUtils;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Tenant tenant;
    private Staff staffA;
    private Staff staffB;
    private User staffUserA;
    private User staffUserB;
    private User ownerUser;
    private String staffAJwt;
    private String staffBJwt;
    private String ownerJwt;

    private Service service;
    private Customer customer;
    private Booking bookingA;
    private Booking bookingB;

    @BeforeEach
    void setUp() {
        objectMapper.findAndRegisterModules();

        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        tenant = new Tenant();
        tenant.setName("TIKEY SPA Test");
        tenant.setSlug("tikey-spa-test-" + System.currentTimeMillis());
        tenant = tenantRepository.saveAndFlush(tenant);

        staffA = new Staff();
        staffA.setTenant(tenant);
        staffA.setName("Staff Member A");
        staffA.setIsActive(true);
        staffA = staffRepository.saveAndFlush(staffA);

        staffB = new Staff();
        staffB.setTenant(tenant);
        staffB.setName("Staff Member B");
        staffB.setIsActive(true);
        staffB = staffRepository.saveAndFlush(staffB);

        staffUserA = new User();
        staffUserA.setUsername("staffA_" + System.currentTimeMillis());
        staffUserA.setPassword("hash");
        staffUserA.setRole(UserRole.STAFF);
        staffUserA.setTenant(tenant);
        staffUserA.setStaff(staffA);
        staffUserA.setIsActive(true);
        staffUserA = userRepository.saveAndFlush(staffUserA);

        staffUserB = new User();
        staffUserB.setUsername("staffB_" + System.currentTimeMillis());
        staffUserB.setPassword("hash");
        staffUserB.setRole(UserRole.STAFF);
        staffUserB.setTenant(tenant);
        staffUserB.setStaff(staffB);
        staffUserB.setIsActive(true);
        staffUserB = userRepository.saveAndFlush(staffUserB);

        ownerUser = new User();
        ownerUser.setUsername("owner_" + System.currentTimeMillis());
        ownerUser.setPassword("hash");
        ownerUser.setRole(UserRole.OWNER);
        ownerUser.setTenant(tenant);
        ownerUser.setIsActive(true);
        ownerUser = userRepository.saveAndFlush(ownerUser);

        CustomUserDetails userDetailsA = new CustomUserDetails(staffUserA);
        staffAJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(userDetailsA, null, userDetailsA.getAuthorities()));

        CustomUserDetails userDetailsB = new CustomUserDetails(staffUserB);
        staffBJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(userDetailsB, null, userDetailsB.getAuthorities()));

        CustomUserDetails userDetailsOwner = new CustomUserDetails(ownerUser);
        ownerJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(userDetailsOwner, null, userDetailsOwner.getAuthorities()));

        customer = new Customer();
        customer.setTenant(tenant);
        customer.setName("Lan Anh");
        customer.setPhone("0987654321");
        customer.setIsActive(true);
        customer = customerRepository.saveAndFlush(customer);

        service = new Service();
        service.setTenant(tenant);
        service.setName("Chăm sóc da mặt chuyên sâu");
        service.setDurationMinutes(60);
        service.setPrice(new BigDecimal("500000.00"));
        service.setIsActive(true);
        service = serviceRepository.saveAndFlush(service);

        LocalDateTime now = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);

        bookingA = new Booking();
        bookingA.setTenant(tenant);
        bookingA.setCustomer(customer);
        bookingA.setStaff(staffA);
        bookingA.setService(service);
        bookingA.setStartTime(now);
        bookingA.setEndTime(now.plusMinutes(60));
        bookingA.setStatus(BookingStatus.PENDING);
        bookingA.setPrice(service.getPrice());
        bookingA = bookingRepository.saveAndFlush(bookingA);

        bookingB = new Booking();
        bookingB.setTenant(tenant);
        bookingB.setCustomer(customer);
        bookingB.setStaff(staffB);
        bookingB.setService(service);
        bookingB.setStartTime(now.plusHours(2));
        bookingB.setEndTime(now.plusHours(3));
        bookingB.setStatus(BookingStatus.CONFIRMED);
        bookingB.setPrice(service.getPrice());
        bookingB = bookingRepository.saveAndFlush(bookingB);
    }

    @Test
    void staffCanAccessOwnBookingsList() throws Exception {
        mockMvc.perform(get("/api/v1/bookings")
                        .header("Authorization", "Bearer " + staffAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(bookingA.getId().intValue())))
                .andExpect(jsonPath("$[0].staffId", is(staffA.getId().intValue())));
    }

    @Test
    void staffQueryingOtherStaffIdViaParamIsRejected() throws Exception {
        // Staff A attempts parameter manipulation to query Staff B's bookings
        mockMvc.perform(get("/api/v1/bookings?staffId=" + staffB.getId())
                        .header("Authorization", "Bearer " + staffAJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void staffCanAccessOwnBookingDetail() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/" + bookingA.getId())
                        .header("Authorization", "Bearer " + staffAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(bookingA.getId().intValue())))
                .andExpect(jsonPath("$.staffName", is("Staff Member A")))
                .andExpect(jsonPath("$.bookingCode").exists());
    }

    @Test
    void staffCannotAccessOtherStaffBookingDetail_ManipulatedBookingId() throws Exception {
        // Staff A attempts deliberate ID manipulation by accessing bookingB's ID
        mockMvc.perform(get("/api/v1/bookings/" + bookingB.getId())
                        .header("Authorization", "Bearer " + staffAJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void staffCanUpdateStatusOfOwnBooking() throws Exception {
        String json = """
                {
                    "status": "CONFIRMED"
                }
                """;

        mockMvc.perform(patch("/api/v1/bookings/" + bookingA.getId() + "/status")
                        .header("Authorization", "Bearer " + staffAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFIRMED")));
    }

    @Test
    void staffCannotUpdateStatusOfOtherStaffBooking() throws Exception {
        String json = """
                {
                    "status": "COMPLETED"
                }
                """;

        // Staff A attempts to modify status of Staff B's booking
        mockMvc.perform(patch("/api/v1/bookings/" + bookingB.getId() + "/status")
                        .header("Authorization", "Bearer " + staffAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    void staffCannotUpdateOrRescheduleOtherStaffBooking() throws Exception {
        LocalDateTime newStart = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        String json = """
                {
                    "customerId": %d,
                    "serviceId": %d,
                    "staffId": %d,
                    "startTime": "%s",
                    "endTime": "%s"
                }
                """.formatted(customer.getId(), service.getId(), staffB.getId(),
                newStart.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                newStart.plusMinutes(60).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // Staff A attempts to update Staff B's booking
        mockMvc.perform(put("/api/v1/bookings/" + bookingB.getId())
                        .header("Authorization", "Bearer " + staffAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    void staffCannotReassignBookingToAnotherStaffViaRequestBody() throws Exception {
        LocalDateTime newStart = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        String json = """
                {
                    "customerId": %d,
                    "serviceId": %d,
                    "staffId": %d,
                    "startTime": "%s",
                    "endTime": "%s"
                }
                """.formatted(customer.getId(), service.getId(), staffB.getId(), // deliberate staffId manipulation
                newStart.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                newStart.plusMinutes(60).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // Staff A attempts to reassign booking A to Staff B
        mockMvc.perform(put("/api/v1/bookings/" + bookingA.getId())
                        .header("Authorization", "Bearer " + staffAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    void staffCannotCreateBookingAssignedToAnotherStaff() throws Exception {
        LocalDateTime newStart = LocalDateTime.now().plusDays(3).withHour(14).withMinute(0).withSecond(0).withNano(0);
        String json = """
                {
                    "customerId": %d,
                    "serviceId": %d,
                    "staffId": %d,
                    "startTime": "%s",
                    "endTime": "%s"
                }
                """.formatted(customer.getId(), service.getId(), staffB.getId(),
                newStart.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                newStart.plusMinutes(60).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // Staff A attempts to create booking assigned to Staff B
        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + staffAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanViewAllBookingsAndAnyBookingDetail() throws Exception {
        mockMvc.perform(get("/api/v1/bookings")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/api/v1/bookings/" + bookingA.getId())
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(bookingA.getId().intValue())));

        mockMvc.perform(get("/api/v1/bookings/" + bookingB.getId())
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(bookingB.getId().intValue())));
    }
}
