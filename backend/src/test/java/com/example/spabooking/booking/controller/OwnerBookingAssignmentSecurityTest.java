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

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class OwnerBookingAssignmentSecurityTest {

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

    private Tenant tenantA;
    private Tenant tenantB;

    private Staff staffA1;
    private Staff staffA2;
    private Staff inactiveStaff;
    private Staff otherTenantStaff;

    private String ownerAJwt;
    private String staffAJwt;

    private Booking booking;

    @BeforeEach
    void setUp() {
        objectMapper.findAndRegisterModules();

        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        tenantA = new Tenant();
        tenantA.setName("TIKEY SPA");
        tenantA.setSlug("tikey-spa-" + System.currentTimeMillis());
        tenantA = tenantRepository.saveAndFlush(tenantA);

        tenantB = new Tenant();
        tenantB.setName("Other Spa");
        tenantB.setSlug("other-spa-" + System.currentTimeMillis());
        tenantB = tenantRepository.saveAndFlush(tenantB);

        staffA1 = new Staff();
        staffA1.setTenant(tenantA);
        staffA1.setName("Staff A1");
        staffA1.setIsActive(true);
        staffA1 = staffRepository.saveAndFlush(staffA1);

        staffA2 = new Staff();
        staffA2.setTenant(tenantA);
        staffA2.setName("Staff A2");
        staffA2.setIsActive(true);
        staffA2 = staffRepository.saveAndFlush(staffA2);

        inactiveStaff = new Staff();
        inactiveStaff.setTenant(tenantA);
        inactiveStaff.setName("Staff Inactive");
        inactiveStaff.setIsActive(false);
        inactiveStaff = staffRepository.saveAndFlush(inactiveStaff);

        otherTenantStaff = new Staff();
        otherTenantStaff.setTenant(tenantB);
        otherTenantStaff.setName("Other Tenant Staff");
        otherTenantStaff.setIsActive(true);
        otherTenantStaff = staffRepository.saveAndFlush(otherTenantStaff);

        User ownerA = new User();
        ownerA.setUsername("ownerA_" + System.currentTimeMillis());
        ownerA.setPassword("hash");
        ownerA.setRole(UserRole.OWNER);
        ownerA.setTenant(tenantA);
        ownerA.setIsActive(true);
        ownerA = userRepository.saveAndFlush(ownerA);

        User staffUserA1 = new User();
        staffUserA1.setUsername("staffA1_" + System.currentTimeMillis());
        staffUserA1.setPassword("hash");
        staffUserA1.setRole(UserRole.STAFF);
        staffUserA1.setTenant(tenantA);
        staffUserA1.setStaff(staffA1);
        staffUserA1.setIsActive(true);
        staffUserA1 = userRepository.saveAndFlush(staffUserA1);

        CustomUserDetails ownerDetails = new CustomUserDetails(ownerA);
        ownerAJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(ownerDetails, null, ownerDetails.getAuthorities()));

        CustomUserDetails staffDetails = new CustomUserDetails(staffUserA1);
        staffAJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(staffDetails, null, staffDetails.getAuthorities()));

        Customer customer = new Customer();
        customer.setTenant(tenantA);
        customer.setName("Khách hàng A");
        customer.setIsActive(true);
        customer = customerRepository.saveAndFlush(customer);

        Service service = new Service();
        service.setTenant(tenantA);
        service.setName("Dịch vụ Body");
        service.setDurationMinutes(60);
        service.setPrice(new BigDecimal("400000.00"));
        service.setIsActive(true);
        service = serviceRepository.saveAndFlush(service);

        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0).withSecond(0).withNano(0);
        booking = new Booking();
        booking.setTenant(tenantA);
        booking.setCustomer(customer);
        booking.setStaff(staffA1);
        booking.setService(service);
        booking.setStartTime(start);
        booking.setEndTime(start.plusMinutes(60));
        booking.setStatus(BookingStatus.PENDING);
        booking.setPrice(service.getPrice());
        booking = bookingRepository.saveAndFlush(booking);
    }

    @Test
    void ownerCanAssignBookingToActiveSameTenantStaff() throws Exception {
        String json = """
                {
                    "staffId": %d
                }
                """.formatted(staffA2.getId());

        mockMvc.perform(patch("/api/v1/bookings/" + booking.getId() + "/assign")
                        .header("Authorization", "Bearer " + ownerAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.staffId", is(staffA2.getId().intValue())))
                .andExpect(jsonPath("$.staffName", is("Staff A2")));
    }

    @Test
    void ownerCannotAssignBookingToInactiveStaff() throws Exception {
        String json = """
                {
                    "staffId": %d
                }
                """.formatted(inactiveStaff.getId());

        mockMvc.perform(patch("/api/v1/bookings/" + booking.getId() + "/assign")
                        .header("Authorization", "Bearer " + ownerAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerCannotAssignBookingToOtherTenantStaff() throws Exception {
        String json = """
                {
                    "staffId": %d
                }
                """.formatted(otherTenantStaff.getId());

        mockMvc.perform(patch("/api/v1/bookings/" + booking.getId() + "/assign")
                        .header("Authorization", "Bearer " + ownerAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    void cannotReassignTerminalBooking() throws Exception {
        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.saveAndFlush(booking);

        String json = """
                {
                    "staffId": %d
                }
                """.formatted(staffA2.getId());

        mockMvc.perform(patch("/api/v1/bookings/" + booking.getId() + "/assign")
                        .header("Authorization", "Bearer " + ownerAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void assignmentDetectsScheduleOverlap() throws Exception {
        // Create an existing booking for staffA2 at the exact same time
        Booking existingA2 = new Booking();
        existingA2.setTenant(tenantA);
        existingA2.setCustomer(booking.getCustomer());
        existingA2.setStaff(staffA2);
        existingA2.setService(booking.getService());
        existingA2.setStartTime(booking.getStartTime());
        existingA2.setEndTime(booking.getEndTime());
        existingA2.setStatus(BookingStatus.CONFIRMED);
        existingA2.setPrice(booking.getPrice());
        bookingRepository.saveAndFlush(existingA2);

        String json = """
                {
                    "staffId": %d
                }
                """.formatted(staffA2.getId());

        mockMvc.perform(patch("/api/v1/bookings/" + booking.getId() + "/assign")
                        .header("Authorization", "Bearer " + ownerAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict());
    }

    @Test
    void staffCannotCallAssignEndpoint() throws Exception {
        String json = """
                {
                    "staffId": %d
                }
                """.formatted(staffA2.getId());

        mockMvc.perform(patch("/api/v1/bookings/" + booking.getId() + "/assign")
                        .header("Authorization", "Bearer " + staffAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }
}
