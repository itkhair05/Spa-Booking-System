package com.example.spabooking.service;

import com.example.spabooking.auth.entity.User;
import com.example.spabooking.auth.enums.UserRole;
import com.example.spabooking.auth.repository.UserRepository;
import com.example.spabooking.auth.security.CustomUserDetails;
import com.example.spabooking.auth.security.JwtUtils;
import com.example.spabooking.customer.entity.Customer;
import com.example.spabooking.customer.repository.CustomerRepository;
import com.example.spabooking.service.entity.Service;
import com.example.spabooking.service.repository.ServiceRepository;
import com.example.spabooking.staff.entity.Staff;
import com.example.spabooking.staff.repository.StaffRepository;
import com.example.spabooking.tenant.context.TenantContext;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class TenantIsolationIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private JwtUtils jwtUtils;

    private MockMvc mockMvc;

    private Tenant tenantA;
    private Tenant tenantB;

    private User userA;
    private User userB;

    private Service serviceA;
    private Service serviceB;

    private Staff staffA;
    private Staff staffB;

    private Customer customerA;
    private Customer customerB;

    private String jwtA;
    private String jwtB;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // 1. Setup Tenants
        tenantA = new Tenant();
        tenantA.setName("Tenant A");
        tenantA.setSlug("tenant-a-it");
        tenantA = tenantRepository.saveAndFlush(tenantA);

        tenantB = new Tenant();
        tenantB.setName("Tenant B");
        tenantB.setSlug("tenant-b-it");
        tenantB = tenantRepository.saveAndFlush(tenantB);

        // 2. Setup Users
        userA = new User();
        userA.setUsername("userA");
        userA.setPassword("encodedPassword");
        userA.setRole(UserRole.OWNER);
        userA.setTenant(tenantA);
        userA = userRepository.saveAndFlush(userA);

        userB = new User();
        userB.setUsername("userB");
        userB.setPassword("encodedPassword");
        userB.setRole(UserRole.OWNER);
        userB.setTenant(tenantB);
        userB = userRepository.saveAndFlush(userB);

        // 3. Setup Data
        serviceA = new Service();
        serviceA.setTenant(tenantA);
        serviceA.setName("Service A");
        serviceA.setDurationMinutes(60);
        serviceA.setPrice(new BigDecimal("100.00"));
        serviceA = serviceRepository.saveAndFlush(serviceA);

        serviceB = new Service();
        serviceB.setTenant(tenantB);
        serviceB.setName("Service B");
        serviceB.setDurationMinutes(30);
        serviceB.setPrice(new BigDecimal("50.00"));
        serviceB = serviceRepository.saveAndFlush(serviceB);

        staffA = new Staff();
        staffA.setTenant(tenantA);
        staffA.setName("Staff A");
        staffA = staffRepository.saveAndFlush(staffA);

        staffB = new Staff();
        staffB.setTenant(tenantB);
        staffB.setName("Staff B");
        staffB = staffRepository.saveAndFlush(staffB);

        customerA = new Customer();
        customerA.setTenant(tenantA);
        customerA.setName("Customer A");
        customerA = customerRepository.saveAndFlush(customerA);

        customerB = new Customer();
        customerB.setTenant(tenantB);
        customerB.setName("Customer B");
        customerB = customerRepository.saveAndFlush(customerB);

        // 4. Generate JWTs
        CustomUserDetails detailsA = new CustomUserDetails(userA);
        UsernamePasswordAuthenticationToken authA = new UsernamePasswordAuthenticationToken(detailsA, null, detailsA.getAuthorities());
        jwtA = jwtUtils.generateJwtToken(authA);

        CustomUserDetails detailsB = new CustomUserDetails(userB);
        UsernamePasswordAuthenticationToken authB = new UsernamePasswordAuthenticationToken(detailsB, null, detailsB.getAuthorities());
        jwtB = jwtUtils.generateJwtToken(authB);
        
        TenantContext.clear();
    }

    @Test
    void testUnauthenticatedAccessDenied() throws Exception {
        mockMvc.perform(get("/api/test-isolation/services/" + serviceA.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testCrossTenantServiceIsolation() throws Exception {
        // Tenant A can access Service A
        mockMvc.perform(get("/api/test-isolation/services/" + serviceA.getId())
                        .header("Authorization", "Bearer " + jwtA))
                .andExpect(status().isOk());

        // Tenant A cannot access Service B
        mockMvc.perform(get("/api/test-isolation/services/" + serviceB.getId())
                        .header("Authorization", "Bearer " + jwtA))
                .andExpect(status().isNotFound());
                
        // Tenant B can access Service B
        mockMvc.perform(get("/api/test-isolation/services/" + serviceB.getId())
                        .header("Authorization", "Bearer " + jwtB))
                .andExpect(status().isOk());

        // Tenant B cannot access Service A
        mockMvc.perform(get("/api/test-isolation/services/" + serviceA.getId())
                        .header("Authorization", "Bearer " + jwtB))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCrossTenantStaffIsolation() throws Exception {
        mockMvc.perform(get("/api/test-isolation/staff/" + staffA.getId())
                        .header("Authorization", "Bearer " + jwtA))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/test-isolation/staff/" + staffB.getId())
                        .header("Authorization", "Bearer " + jwtA))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCrossTenantCustomerIsolation() throws Exception {
        mockMvc.perform(get("/api/test-isolation/customers/" + customerA.getId())
                        .header("Authorization", "Bearer " + jwtA))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/test-isolation/customers/" + customerB.getId())
                        .header("Authorization", "Bearer " + jwtA))
                .andExpect(status().isNotFound());
    }

    @Test
    void testClientSuppliedTenantIdAttack() throws Exception {
        // User A tries to pass X-Tenant-ID header to access Tenant B's service
        mockMvc.perform(get("/api/test-isolation/services/" + serviceB.getId())
                        .header("Authorization", "Bearer " + jwtA)
                        .header("X-Tenant-ID", tenantB.getId().toString()))
                .andExpect(status().isNotFound());

        // Also via query param
        mockMvc.perform(get("/api/test-isolation/services/" + serviceB.getId() + "?tenantId=" + tenantB.getId())
                        .header("Authorization", "Bearer " + jwtA))
                .andExpect(status().isNotFound());
    }
}
