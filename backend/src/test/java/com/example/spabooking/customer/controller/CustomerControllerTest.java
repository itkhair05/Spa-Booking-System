package com.example.spabooking.customer.controller;

import com.example.spabooking.auth.entity.User;
import com.example.spabooking.auth.enums.UserRole;
import com.example.spabooking.auth.repository.UserRepository;
import com.example.spabooking.auth.security.CustomUserDetails;
import com.example.spabooking.auth.security.JwtUtils;
import com.example.spabooking.customer.entity.Customer;
import com.example.spabooking.customer.repository.CustomerRepository;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class CustomerControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private CustomerRepository customerRepository;

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
    private Customer customerB;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        tenantA = new Tenant();
        tenantA.setName("Tenant A");
        tenantA.setSlug("tenant-a-customer");
        tenantA = tenantRepository.saveAndFlush(tenantA);

        tenantB = new Tenant();
        tenantB.setName("Tenant B");
        tenantB.setSlug("tenant-b-customer");
        tenantB = tenantRepository.saveAndFlush(tenantB);

        User ownerA = new User();
        ownerA.setUsername("ownerACust");
        ownerA.setPassword("encoded");
        ownerA.setRole(UserRole.OWNER);
        ownerA.setTenant(tenantA);
        ownerA = userRepository.saveAndFlush(ownerA);

        User staffUserA = new User();
        staffUserA.setUsername("staffACust");
        staffUserA.setPassword("encoded");
        staffUserA.setRole(UserRole.STAFF);
        staffUserA.setTenant(tenantA);
        staffUserA = userRepository.saveAndFlush(staffUserA);

        User ownerB = new User();
        ownerB.setUsername("ownerBCust");
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
        customerA.setPhone("123456789");
        customerA.setEmail("alice@example.com");
        customerA = customerRepository.saveAndFlush(customerA);

        customerB = new Customer();
        customerB.setTenant(tenantB);
        customerB.setName("Bob");
        customerB.setPhone("987654321");
        customerB.setEmail("bob@example.com");
        customerB = customerRepository.saveAndFlush(customerB);
    }

    @Test
    void testUnauthenticatedGet() throws Exception {
        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testOwnerGetCustomers() throws Exception {
        mockMvc.perform(get("/api/v1/customers")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Alice")));
    }

    @Test
    void testStaffCannotGetCustomers() throws Exception {
        mockMvc.perform(get("/api/v1/customers")
                        .header("Authorization", "Bearer " + staffJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void testCrossTenantGet() throws Exception {
        mockMvc.perform(get("/api/v1/customers/" + customerB.getId())
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void testOwnerCreateCustomer() throws Exception {
        String requestJson = """
                {
                    "name": "Charlie",
                    "email": "charlie@example.com",
                    "phone": "555-0100"
                }
                """;

        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Charlie")));
    }

    @Test
    void testStaffCannotCreateCustomer() throws Exception {
        String requestJson = """
                {
                    "name": "Charlie",
                    "email": "charlie@example.com",
                    "phone": "555-0100"
                }
                """;

        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer " + staffJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isForbidden());
    }

    @Test
    void testStaffCannotUpdateCustomer() throws Exception {
        String requestJson = """
                {
                    "name": "Alice Updated",
                    "email": "alice.updated@example.com",
                    "phone": "123456789"
                }
                """;

        mockMvc.perform(put("/api/v1/customers/" + customerA.getId())
                        .header("Authorization", "Bearer " + staffJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isForbidden());
    }

    @Test
    void testOwnerUpdateCustomer() throws Exception {
        String requestJson = """
                {
                    "name": "Alice Updated",
                    "email": "alice.updated@example.com",
                    "phone": "123456789"
                }
                """;

        mockMvc.perform(put("/api/v1/customers/" + customerA.getId())
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Alice Updated")));
    }

    @Test
    void testCrossTenantUpdate() throws Exception {
        String requestJson = """
                {
                    "name": "Hacked Bob",
                    "email": "hacked@example.com"
                }
                """;

        mockMvc.perform(put("/api/v1/customers/" + customerB.getId())
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound());
    }

    @Test
    void testOwnerDeleteCustomer() throws Exception {
        mockMvc.perform(delete("/api/v1/customers/" + customerA.getId())
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/customers/" + customerA.getId())
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isNotFound());
                
        // Should not appear in list either
        mockMvc.perform(get("/api/v1/customers")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testStaffCannotDeleteCustomer() throws Exception {
        mockMvc.perform(delete("/api/v1/customers/" + customerA.getId())
                        .header("Authorization", "Bearer " + staffJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void testCrossTenantDelete() throws Exception {
        mockMvc.perform(delete("/api/v1/customers/" + customerB.getId())
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void testValidationFailure() throws Exception {
        String requestJson = """
                {
                    "name": "",
                    "email": "invalid-email"
                }
                """;

        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists());
    }
}
