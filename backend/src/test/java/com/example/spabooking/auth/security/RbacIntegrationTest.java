package com.example.spabooking.auth.security;

import com.example.spabooking.auth.entity.User;
import com.example.spabooking.auth.enums.UserRole;
import com.example.spabooking.auth.repository.UserRepository;
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

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class RbacIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private JwtUtils jwtUtils;

    private MockMvc mockMvc;

    private String ownerJwt;
    private String staffJwt;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        Tenant tenant = new Tenant();
        tenant.setName("Test Tenant");
        tenant.setSlug("test-tenant");
        tenant = tenantRepository.saveAndFlush(tenant);

        User owner = new User();
        owner.setUsername("owner_user");
        owner.setPassword("encoded");
        owner.setRole(UserRole.OWNER);
        owner.setTenant(tenant);
        owner = userRepository.saveAndFlush(owner);

        User staff = new User();
        staff.setUsername("staff_user");
        staff.setPassword("encoded");
        staff.setRole(UserRole.STAFF);
        staff.setTenant(tenant);
        staff = userRepository.saveAndFlush(staff);

        CustomUserDetails ownerDetails = new CustomUserDetails(owner);
        UsernamePasswordAuthenticationToken authOwner = new UsernamePasswordAuthenticationToken(ownerDetails, null, ownerDetails.getAuthorities());
        ownerJwt = jwtUtils.generateJwtToken(authOwner);

        CustomUserDetails staffDetails = new CustomUserDetails(staff);
        UsernamePasswordAuthenticationToken authStaff = new UsernamePasswordAuthenticationToken(staffDetails, null, staffDetails.getAuthorities());
        staffJwt = jwtUtils.generateJwtToken(authStaff);
    }

    @Test
    void testUnauthenticatedGets401() throws Exception {
        // anyRequest().authenticated() in SecurityConfig applies before @PreAuthorize
        mockMvc.perform(get("/api/test-rbac/owner-only"))
                .andExpect(status().isUnauthorized()); // 401
    }

    @Test
    void testOwnerAccessesOwnerOnly() throws Exception {
        mockMvc.perform(get("/api/test-rbac/owner-only")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk());
    }

    @Test
    void testOwnerAccessesStaffCompatible() throws Exception {
        mockMvc.perform(get("/api/test-rbac/staff-compatible")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk());
    }

    @Test
    void testStaffAccessesStaffCompatible() throws Exception {
        mockMvc.perform(get("/api/test-rbac/staff-compatible")
                        .header("Authorization", "Bearer " + staffJwt))
                .andExpect(status().isOk());
    }

    @Test
    void testStaffDeniedOwnerOnly() throws Exception {
        mockMvc.perform(get("/api/test-rbac/owner-only")
                        .header("Authorization", "Bearer " + staffJwt))
                .andExpect(status().isForbidden()); // 403
    }

    @Test
    void testRoleSpoofingAttackDenied() throws Exception {
        // Staff user tries to escalate by passing a role header
        mockMvc.perform(get("/api/test-rbac/owner-only")
                        .header("Authorization", "Bearer " + staffJwt)
                        .header("X-Role", "OWNER")
                        .header("role", "ROLE_OWNER"))
                .andExpect(status().isForbidden());
    }
}
