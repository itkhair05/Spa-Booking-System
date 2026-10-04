package com.example.spabooking.auth;

import com.example.spabooking.auth.dto.ChangePasswordRequest;
import com.example.spabooking.auth.dto.LoginRequest;
import com.example.spabooking.auth.entity.User;
import com.example.spabooking.auth.enums.UserRole;
import com.example.spabooking.auth.repository.UserRepository;
import com.example.spabooking.auth.security.CustomUserDetails;
import com.example.spabooking.auth.security.JwtUtils;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ChangePasswordSecurityTest {

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

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    private Tenant tenant;
    private User ownerUser;
    private User staffUser;

    private String ownerJwt;
    private String staffJwt;

    private final String OLD_PASSWORD = "OldPassword123!";
    private final String NEW_PASSWORD = "NewPassword456!";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        tenant = new Tenant();
        tenant.setName("Password Test Spa");
        tenant.setSlug("pw-test-" + UUID.randomUUID());
        tenant = tenantRepository.saveAndFlush(tenant);

        ownerUser = new User();
        ownerUser.setUsername("owner_pw_" + UUID.randomUUID());
        ownerUser.setPassword(passwordEncoder.encode(OLD_PASSWORD));
        ownerUser.setRole(UserRole.OWNER);
        ownerUser.setTenant(tenant);
        ownerUser = userRepository.saveAndFlush(ownerUser);

        Staff staff = new Staff();
        staff.setName("Staff PW");
        staff.setTenant(tenant);
        staff.setIsActive(true);
        staff = staffRepository.saveAndFlush(staff);

        staffUser = new User();
        staffUser.setUsername("staff_pw_" + UUID.randomUUID());
        staffUser.setPassword(passwordEncoder.encode(OLD_PASSWORD));
        staffUser.setRole(UserRole.STAFF);
        staffUser.setTenant(tenant);
        staffUser.setStaff(staff);
        staffUser = userRepository.saveAndFlush(staffUser);

        ownerJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(ownerUser), null, new CustomUserDetails(ownerUser).getAuthorities()));
        staffJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(staffUser), null, new CustomUserDetails(staffUser).getAuthorities()));
    }

    @Test
    void testOwnerCanChangeOwnPasswordSuccessfully() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest(OLD_PASSWORD, NEW_PASSWORD, NEW_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đổi mật khẩu thành công"));

        // Login with new password must succeed
        LoginRequest loginRequest = new LoginRequest(ownerUser.getUsername(), NEW_PASSWORD);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    void testStaffCanChangeOwnPasswordSuccessfully() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest(OLD_PASSWORD, NEW_PASSWORD, NEW_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + staffJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đổi mật khẩu thành công"));

        // Login with new password must succeed
        LoginRequest loginRequest = new LoginRequest(staffUser.getUsername(), NEW_PASSWORD);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    void testChangePasswordWithIncorrectCurrentPasswordRejected() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("WrongPassword!", NEW_PASSWORD, NEW_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + staffJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Mật khẩu hiện tại không chính xác"));
    }

    @Test
    void testChangePasswordWithMismatchedConfirmationRejected() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest(OLD_PASSWORD, NEW_PASSWORD, "DifferentPassword!");

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + staffJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Mật khẩu xác nhận không khớp với mật khẩu mới"));
    }

    @Test
    void testChangePasswordWithTooShortNewPasswordRejected() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest(OLD_PASSWORD, "123", "123");

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + staffJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUnauthenticatedChangePasswordRejected() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest(OLD_PASSWORD, NEW_PASSWORD, NEW_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
