package com.example.spabooking.staff;

import com.example.spabooking.auth.dto.LoginRequest;
import com.example.spabooking.auth.entity.User;
import com.example.spabooking.auth.enums.UserRole;
import com.example.spabooking.auth.repository.UserRepository;
import com.example.spabooking.auth.security.CustomUserDetails;
import com.example.spabooking.auth.security.JwtUtils;
import com.example.spabooking.staff.dto.UpdateStaffSelfProfileRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class StaffSelfProfileSecurityTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Tenant tenant;
    private Staff staff;
    private User staffUser;
    private String staffPassword = "initialStaffPass123";
    private String staffJwt;

    private Staff otherStaff;
    private User otherStaffUser;

    private User ownerUser;
    private String ownerJwt;

    @BeforeEach
    void setUp() {
        objectMapper.findAndRegisterModules();

        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        long unique = System.currentTimeMillis();

        tenant = new Tenant();
        tenant.setName("TIKEY SPA Profile Test");
        tenant.setSlug("tikey-profile-" + unique);
        tenant = tenantRepository.saveAndFlush(tenant);

        staff = new Staff();
        staff.setTenant(tenant);
        staff.setName("Nguyễn Linh");
        staff.setPhone("0911222333");
        staff.setEmail("linh.nguyen@tikey.local");
        staff.setIsActive(true);
        staff.setIsDeleted(false);
        staff = staffRepository.saveAndFlush(staff);

        staffUser = new User();
        staffUser.setUsername("linh.nguyen@tikey.local");
        staffUser.setPassword(passwordEncoder.encode(staffPassword));
        staffUser.setRole(UserRole.STAFF);
        staffUser.setTenant(tenant);
        staffUser.setStaff(staff);
        staffUser.setIsActive(true);
        staffUser = userRepository.saveAndFlush(staffUser);

        CustomUserDetails staffDetails = new CustomUserDetails(staffUser);
        staffJwt = jwtUtils.generateJwtToken(
                new UsernamePasswordAuthenticationToken(staffDetails, null, staffDetails.getAuthorities()));

        otherStaff = new Staff();
        otherStaff.setTenant(tenant);
        otherStaff.setName("Lê Mai");
        otherStaff.setPhone("0944555666");
        otherStaff.setEmail("mai.le@tikey.local");
        otherStaff.setIsActive(true);
        otherStaff.setIsDeleted(false);
        otherStaff = staffRepository.saveAndFlush(otherStaff);

        otherStaffUser = new User();
        otherStaffUser.setUsername("mai.le@tikey.local");
        otherStaffUser.setPassword(passwordEncoder.encode("maiPassword123"));
        otherStaffUser.setRole(UserRole.STAFF);
        otherStaffUser.setTenant(tenant);
        otherStaffUser.setStaff(otherStaff);
        otherStaffUser.setIsActive(true);
        otherStaffUser = userRepository.saveAndFlush(otherStaffUser);

        ownerUser = new User();
        ownerUser.setUsername("owner_profile_" + unique);
        ownerUser.setPassword(passwordEncoder.encode("ownerPass123"));
        ownerUser.setRole(UserRole.OWNER);
        ownerUser.setTenant(tenant);
        ownerUser.setIsActive(true);
        ownerUser = userRepository.saveAndFlush(ownerUser);

        CustomUserDetails ownerDetails = new CustomUserDetails(ownerUser);
        ownerJwt = jwtUtils.generateJwtToken(
                new UsernamePasswordAuthenticationToken(ownerDetails, null, ownerDetails.getAuthorities()));
    }

    @Test
    void staffCanRetrieveOwnProfile() throws Exception {
        mockMvc.perform(get("/api/v1/staff/me")
                        .header("Authorization", "Bearer " + staffJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(staff.getId().intValue())))
                .andExpect(jsonPath("$.name", is("Nguyễn Linh")))
                .andExpect(jsonPath("$.phone", is("0911222333")))
                .andExpect(jsonPath("$.email", is("linh.nguyen@tikey.local")));
    }

    @Test
    void staffCanUpdateOwnNameAndPhone() throws Exception {
        UpdateStaffSelfProfileRequest request = new UpdateStaffSelfProfileRequest(
                "Trần Linh", "0988999888", "linh.nguyen@tikey.local");

        mockMvc.perform(put("/api/v1/staff/me")
                        .header("Authorization", "Bearer " + staffJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Trần Linh")))
                .andExpect(jsonPath("$.phone", is("0988999888")));

        Staff persisted = staffRepository.findById(staff.getId()).orElseThrow();
        assertEquals("Trần Linh", persisted.getName());
        assertEquals("0988999888", persisted.getPhone());
    }

    @Test
    void staffCanUpdateOwnEmailAndLoginWithNewEmail() throws Exception {
        String newEmail = "linh.new@tikeyspa.vn";
        UpdateStaffSelfProfileRequest request = new UpdateStaffSelfProfileRequest(
                "Nguyễn Linh", "0911222333", newEmail);

        MvcResult result = mockMvc.perform(put("/api/v1/staff/me")
                        .header("Authorization", "Bearer " + staffJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is(newEmail)))
                .andExpect(jsonPath("$.username", is(newEmail)))
                .andReturn();

        // 1. New email can be used to login
        LoginRequest loginWithNewEmail = new LoginRequest(newEmail, staffPassword);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginWithNewEmail)))
                .andExpect(status().isOk());

        // 2. Old email is rejected
        LoginRequest loginWithOldEmail = new LoginRequest("linh.nguyen@tikey.local", staffPassword);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginWithOldEmail)))
                .andExpect(status().isUnauthorized());

        // 3. User account is updated without creating duplicate User
        assertEquals(1, userRepository.findAll().stream()
                .filter(u -> u.getStaff() != null && u.getStaff().getId().equals(staff.getId()))
                .count());

        // 4. Role and tenant remain strictly unchanged
        User updatedUser = userRepository.findByUsername(newEmail).orElseThrow();
        assertEquals(UserRole.STAFF, updatedUser.getRole());
        assertEquals(tenant.getId(), updatedUser.getTenant().getId());
    }

    @Test
    void staffCannotUpdateEmailToExistingUserEmail() throws Exception {
        // Try to take otherStaff's email
        UpdateStaffSelfProfileRequest request = new UpdateStaffSelfProfileRequest(
                "Nguyễn Linh", "0911222333", "mai.le@tikey.local");

        mockMvc.perform(put("/api/v1/staff/me")
                        .header("Authorization", "Bearer " + staffJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void staffCannotUpdateAnotherStaffProfile() throws Exception {
        // STAFF tries to call PUT /api/v1/staff/{otherStaff.id}
        mockMvc.perform(put("/api/v1/staff/" + otherStaff.getId())
                        .header("Authorization", "Bearer " + staffJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hacked Name\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerStaffListReflectsStaffProfileUpdates() throws Exception {
        UpdateStaffSelfProfileRequest request = new UpdateStaffSelfProfileRequest(
                "Trần Thị Linh", "0987654321", "linh.tran@tikeyspa.vn");

        mockMvc.perform(put("/api/v1/staff/me")
                        .header("Authorization", "Bearer " + staffJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // OWNER fetches staff list: should see updated name, phone, email
        mockMvc.perform(get("/api/v1/staff")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + staff.getId() + ")].name").value("Trần Thị Linh"))
                .andExpect(jsonPath("$[?(@.id == " + staff.getId() + ")].phone").value("0987654321"))
                .andExpect(jsonPath("$[?(@.id == " + staff.getId() + ")].username").value("linh.tran@tikeyspa.vn"));
    }

    @Test
    void ownerCanDeleteAndStaffMarkedInactiveInOwnerList() throws Exception {
        // OWNER calls DELETE /api/v1/staff/{id}
        mockMvc.perform(delete("/api/v1/staff/" + otherStaff.getId())
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isNoContent());

        // Staff entity still exists in DB but isDeleted = true, isActive = false
        Staff deletedStaff = staffRepository.findById(otherStaff.getId()).orElseThrow();
        assertTrue(deletedStaff.getIsDeleted());
        assertFalse(deletedStaff.getIsActive());

        // Linked User is deactivated
        User deletedUser = userRepository.findByUsername("mai.le@tikey.local").orElseThrow();
        assertFalse(deletedUser.getIsActive());

        // Login is rejected
        LoginRequest loginReq = new LoginRequest("mai.le@tikey.local", "maiPassword123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized());

        // Deleted staff remains in OWNER staff list with isActive = false, isDeleted = true
        mockMvc.perform(get("/api/v1/staff")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + otherStaff.getId() + ")].isActive").value(false))
                .andExpect(jsonPath("$[?(@.id == " + otherStaff.getId() + ")].isDeleted").value(true));
    }
}
