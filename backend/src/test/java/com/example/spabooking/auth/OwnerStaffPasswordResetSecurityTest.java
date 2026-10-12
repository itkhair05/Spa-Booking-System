package com.example.spabooking.auth;

import com.example.spabooking.auth.dto.ChangePasswordRequest;
import com.example.spabooking.auth.dto.ForgotPasswordRequest;
import com.example.spabooking.auth.dto.LoginRequest;
import com.example.spabooking.auth.dto.ResetPasswordRequest;
import com.example.spabooking.auth.entity.PasswordResetToken;
import com.example.spabooking.auth.entity.User;
import com.example.spabooking.auth.enums.UserRole;
import com.example.spabooking.auth.repository.PasswordResetTokenRepository;
import com.example.spabooking.auth.repository.UserRepository;
import com.example.spabooking.auth.security.JwtUtils;
import com.example.spabooking.auth.service.PasswordResetService;
import com.example.spabooking.staff.entity.Staff;
import com.example.spabooking.staff.repository.StaffRepository;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "security.rate-limit.enabled=true")
@Transactional
public class OwnerStaffPasswordResetSecurityTest {

    private static final AtomicInteger ipCounter = new AtomicInteger(100);

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StaffRepository staffRepository;
    @Autowired
    private PasswordResetTokenRepository tokenRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private PasswordResetService passwordResetService;
    @Autowired
    private JwtUtils jwtUtils;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    private Tenant tenant;
    private User ownerUser;
    private Staff staff;
    private User staffUser;

    private static RequestPostProcessor uniqueIp() {
        int id = ipCounter.incrementAndGet();
        String ip = "10.5." + (id / 250) + "." + (id % 250);
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    private static RequestPostProcessor fromIp(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    private static void awaitFreshWindow() throws InterruptedException {
        long intoWindow = System.currentTimeMillis() % 60_000;
        if (intoWindow > 55_000) {
            Thread.sleep(60_000 - intoWindow);
        }
    }

    private MockHttpServletRequestBuilder postApi(String url) {
        return post(url).with(uniqueIp());
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        String suffix = UUID.randomUUID().toString().substring(0, 8);

        tenant = new Tenant();
        tenant.setName("Spa " + suffix);
        tenant.setSlug("spa-" + suffix);
        tenant.setEmail("tenant-" + suffix + "@example.com");
        tenant.setIsActive(true);
        tenant = tenantRepository.save(tenant);

        ownerUser = new User();
        ownerUser.setTenant(tenant);
        ownerUser.setUsername("owner-" + suffix + "@example.com");
        ownerUser.setPassword(passwordEncoder.encode("oldPassword123"));
        ownerUser.setRole(UserRole.OWNER);
        ownerUser.setIsActive(true);
        ownerUser.setTokenVersion(0);
        ownerUser = userRepository.save(ownerUser);

        staff = new Staff();
        staff.setTenant(tenant);
        staff.setName("Staff " + suffix);
        staff.setEmail("staff-" + suffix + "@example.com");
        staff.setIsActive(true);
        staff.setIsDeleted(false);
        staff = staffRepository.save(staff);

        staffUser = new User();
        staffUser.setTenant(tenant);
        staffUser.setUsername("staffuser-" + suffix);
        staffUser.setPassword(passwordEncoder.encode("oldPassword123"));
        staffUser.setRole(UserRole.STAFF);
        staffUser.setStaff(staff);
        staffUser.setIsActive(true);
        staffUser.setTokenVersion(0);
        staffUser = userRepository.save(staffUser);
    }

    @Test
    void ownerCanRequestReset_returnsGenericMessage_tokenStoredHashed() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest(tenant.getEmail());

        mockMvc.perform(postApi("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(PasswordResetService.GENERIC_FORGOT_SUCCESS_MESSAGE));

        List<PasswordResetToken> tokens = tokenRepository.findAll()
                .stream()
                .filter(t -> t.getUser().getId().equals(ownerUser.getId()))
                .toList();

        assertThat(tokens).hasSize(1);
        PasswordResetToken token = tokens.get(0);
        assertThat(token.getTokenHash()).hasSize(64);
        assertThat(token.getUsedAt()).isNull();
        assertThat(token.getExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(14));
    }

    @Test
    void ownerCanRequestReset_usingUsername_returnsGenericMessage() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest(ownerUser.getUsername());

        mockMvc.perform(postApi("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(PasswordResetService.GENERIC_FORGOT_SUCCESS_MESSAGE));

        List<PasswordResetToken> tokens = tokenRepository.findAll()
                .stream()
                .filter(t -> t.getUser().getId().equals(ownerUser.getId()))
                .toList();

        assertThat(tokens).isNotEmpty();
    }

    @Test
    void staffCanRequestReset_returnsGenericMessage_tokenStoredHashed() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest(staff.getEmail());

        mockMvc.perform(postApi("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(PasswordResetService.GENERIC_FORGOT_SUCCESS_MESSAGE));

        List<PasswordResetToken> tokens = tokenRepository.findAll()
                .stream()
                .filter(t -> t.getUser().getId().equals(staffUser.getId()))
                .toList();

        assertThat(tokens).hasSize(1);
        PasswordResetToken token = tokens.get(0);
        assertThat(token.getTokenHash()).hasSize(64);
        assertThat(token.getUsedAt()).isNull();
    }

    @Test
    void unknownEmail_returnsSameGenericResponse_noTokenGenerated() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("nonexistent@example.com");

        mockMvc.perform(postApi("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(PasswordResetService.GENERIC_FORGOT_SUCCESS_MESSAGE));

        List<PasswordResetToken> tokens = tokenRepository.findAll();
        assertThat(tokens).isEmpty();
    }

    @Test
    void inactiveUser_cannotReset_noTokenGenerated() throws Exception {
        ownerUser.setIsActive(false);
        userRepository.save(ownerUser);

        ForgotPasswordRequest request = new ForgotPasswordRequest(tenant.getEmail());

        mockMvc.perform(postApi("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(PasswordResetService.GENERIC_FORGOT_SUCCESS_MESSAGE));

        List<PasswordResetToken> tokens = tokenRepository.findAll()
                .stream()
                .filter(t -> t.getUser().getId().equals(ownerUser.getId()))
                .toList();
        assertThat(tokens).isEmpty();
    }

    @Test
    void inactiveTenant_cannotReset_noTokenGenerated() throws Exception {
        tenant.setIsActive(false);
        tenantRepository.save(tenant);

        ForgotPasswordRequest request = new ForgotPasswordRequest(ownerUser.getUsername());

        mockMvc.perform(postApi("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(PasswordResetService.GENERIC_FORGOT_SUCCESS_MESSAGE));

        List<PasswordResetToken> tokens = tokenRepository.findAll()
                .stream()
                .filter(t -> t.getUser().getId().equals(ownerUser.getId()))
                .toList();
        assertThat(tokens).isEmpty();
    }

    @Test
    void inactiveOrDeletedStaff_cannotReset_noTokenGenerated() throws Exception {
        staff.setIsActive(false);
        staffRepository.save(staff);

        ForgotPasswordRequest request = new ForgotPasswordRequest(staff.getEmail());

        mockMvc.perform(postApi("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(PasswordResetService.GENERIC_FORGOT_SUCCESS_MESSAGE));

        List<PasswordResetToken> tokens = tokenRepository.findAll()
                .stream()
                .filter(t -> t.getUser().getId().equals(staffUser.getId()))
                .toList();
        assertThat(tokens).isEmpty();
    }

    @Test
    void ambiguousEmail_matchingMultipleDistinctAccounts_suppressesResetToken() throws Exception {
        String sharedEmail = "shared-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        tenant.setEmail(sharedEmail);
        tenantRepository.save(tenant);

        staff.setEmail(sharedEmail);
        staffRepository.save(staff);

        ForgotPasswordRequest request = new ForgotPasswordRequest(sharedEmail);

        mockMvc.perform(postApi("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(PasswordResetService.GENERIC_FORGOT_SUCCESS_MESSAGE));

        List<PasswordResetToken> tokens = tokenRepository.findAll();
        assertThat(tokens).isEmpty();
    }

    @Test
    void issuingNewToken_invalidatesPreviousUnusedTokenForSameAccount() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest(tenant.getEmail());

        // First request
        mockMvc.perform(postApi("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        List<PasswordResetToken> firstTokens = tokenRepository.findAll()
                .stream()
                .filter(t -> t.getUser().getId().equals(ownerUser.getId()))
                .toList();
        assertThat(firstTokens).hasSize(1);
        PasswordResetToken firstToken = firstTokens.get(0);
        assertThat(firstToken.getUsedAt()).isNull();

        // Second request
        mockMvc.perform(postApi("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        List<PasswordResetToken> allTokens = tokenRepository.findAll()
                .stream()
                .filter(t -> t.getUser().getId().equals(ownerUser.getId()))
                .toList();
        assertThat(allTokens).hasSize(2);

        PasswordResetToken reloadedFirst = tokenRepository.findById(firstToken.getId()).orElseThrow();
        assertThat(reloadedFirst.getUsedAt()).isNotNull();

        PasswordResetToken secondToken = allTokens.stream()
                .filter(t -> !t.getId().equals(firstToken.getId()))
                .findFirst().orElseThrow();
        assertThat(secondToken.getUsedAt()).isNull();
    }

    @Test
    void resetPassword_validToken_updatesBcryptPassword_andIncrementsTokenVersion() throws Exception {
        String rawToken = "raw-token-32-chars-length-for-testing-only";
        String tokenHash = PasswordResetService.hashToken(rawToken);

        PasswordResetToken token = new PasswordResetToken(ownerUser, tokenHash, LocalDateTime.now().plusMinutes(15));
        tokenRepository.save(token);

        ResetPasswordRequest resetRequest = new ResetPasswordRequest(rawToken, "brandNewPassword123");

        mockMvc.perform(postApi("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(PasswordResetService.RESET_SUCCESS_MESSAGE));

        User updatedUser = userRepository.findById(ownerUser.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("brandNewPassword123", updatedUser.getPassword())).isTrue();
        assertThat(updatedUser.getTokenVersion()).isEqualTo(1);

        PasswordResetToken updatedToken = tokenRepository.findById(token.getId()).orElseThrow();
        assertThat(updatedToken.getUsedAt()).isNotNull();
    }

    @Test
    void resetPassword_loginSucceedsWithNewPassword_failsWithOldPassword() throws Exception {
        String rawToken = "raw-test-token-login-verification";
        String tokenHash = PasswordResetService.hashToken(rawToken);

        PasswordResetToken token = new PasswordResetToken(ownerUser, tokenHash, LocalDateTime.now().plusMinutes(15));
        tokenRepository.save(token);

        ResetPasswordRequest resetRequest = new ResetPasswordRequest(rawToken, "newSuperSecret999");
        mockMvc.perform(postApi("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isOk());

        // Login with old password -> fails (401)
        LoginRequest oldLogin = new LoginRequest(ownerUser.getUsername(), "oldPassword123");
        mockMvc.perform(postApi("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oldLogin)))
                .andExpect(status().isUnauthorized());

        // Login with new password -> succeeds (200)
        LoginRequest newLogin = new LoginRequest(ownerUser.getUsername(), "newSuperSecret999");
        mockMvc.perform(postApi("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    void jwtRevocation_existingJwtRejectedAfterReset() throws Exception {
        // 1. Login before reset to get JWT (tokenVersion = 0)
        LoginRequest loginReq = new LoginRequest(ownerUser.getUsername(), "oldPassword123");
        String loginResponse = mockMvc.perform(postApi("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String jwt = objectMapper.readTree(loginResponse).get("accessToken").asText();
        assertThat(jwt).isNotBlank();

        // 2. Perform authenticated request with current JWT -> succeeds
        ChangePasswordRequest testChangeReq = new ChangePasswordRequest("oldPassword123", "tempPassword456", "tempPassword456");
        mockMvc.perform(postApi("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testChangeReq)))
                .andExpect(status().isOk());

        // 3. Reset password -> increments tokenVersion to 1
        String rawToken = "jwt-revocation-test-token";
        tokenRepository.save(new PasswordResetToken(ownerUser, PasswordResetService.hashToken(rawToken), LocalDateTime.now().plusMinutes(15)));

        ResetPasswordRequest resetReq = new ResetPasswordRequest(rawToken, "brandNewSecret456");
        mockMvc.perform(postApi("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isOk());

        // 4. Use OLD JWT to access protected endpoint -> MUST BE REJECTED (401)
        mockMvc.perform(postApi("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testChangeReq)))
                .andExpect(status().isUnauthorized());

        // 5. New login receives JWT with new tokenVersion -> succeeds
        LoginRequest newLoginReq = new LoginRequest(ownerUser.getUsername(), "brandNewSecret456");
        String newLoginResponse = mockMvc.perform(postApi("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLoginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String newJwt = objectMapper.readTree(newLoginResponse).get("accessToken").asText();
        ChangePasswordRequest changeAgainReq = new ChangePasswordRequest("brandNewSecret456", "anotherPassword789", "anotherPassword789");
        mockMvc.perform(postApi("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + newJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeAgainReq)))
                .andExpect(status().isOk());
    }

    @Test
    void resetPassword_cannotConsumeTokenTwice() throws Exception {
        String rawToken = "single-use-token-test";
        tokenRepository.save(new PasswordResetToken(ownerUser, PasswordResetService.hashToken(rawToken), LocalDateTime.now().plusMinutes(15)));

        ResetPasswordRequest req = new ResetPasswordRequest(rawToken, "validPass123");

        // First consumption
        mockMvc.perform(postApi("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Second consumption -> 400 Bad Request
        mockMvc.perform(postApi("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(PasswordResetService.GENERIC_RESET_ERROR_MESSAGE));
    }

    @Test
    void resetPassword_expiredToken_rejected() throws Exception {
        String rawToken = "expired-token-test";
        PasswordResetToken expiredToken = new PasswordResetToken(ownerUser, PasswordResetService.hashToken(rawToken), LocalDateTime.now().minusMinutes(1));
        tokenRepository.save(expiredToken);

        ResetPasswordRequest req = new ResetPasswordRequest(rawToken, "validPass123");

        mockMvc.perform(postApi("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(PasswordResetService.GENERIC_RESET_ERROR_MESSAGE));
    }

    @Test
    void resetPassword_passwordTooShort_rejected() throws Exception {
        String rawToken = "valid-token-short-pass";
        tokenRepository.save(new PasswordResetToken(ownerUser, PasswordResetService.hashToken(rawToken), LocalDateTime.now().plusMinutes(15)));

        ResetPasswordRequest req = new ResetPasswordRequest(rawToken, "123");

        mockMvc.perform(postApi("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetUrl_cannotBeManipulatedThroughHostOrForwardedHeaders() {
        String rawToken = "safe-token-for-url-test";
        String url = passwordResetService.buildResetUrl(rawToken);

        assertThat(url).startsWith("http://localhost:5173/reset-password?token=");
        assertThat(url).contains(rawToken);
    }

    @Test
    void atomicTokenConsumption_secondAttemptYieldsZeroUpdatedRows() {
        String rawToken = "atomic-token-" + UUID.randomUUID();
        String tokenHash = PasswordResetService.hashToken(rawToken);
        tokenRepository.save(new PasswordResetToken(ownerUser, tokenHash, LocalDateTime.now().plusMinutes(15)));

        LocalDateTime now = LocalDateTime.now();
        int firstUpdate = tokenRepository.markTokenAsUsedIfValid(tokenHash, now);
        int secondUpdate = tokenRepository.markTokenAsUsedIfValid(tokenHash, now);

        assertThat(firstUpdate).isEqualTo(1);
        assertThat(secondUpdate).isEqualTo(0);
    }

    @Test
    void rateLimit_forgotPassword_exceedsFiveRequests_returns429() throws Exception {
        awaitFreshWindow();
        String testIp = "198.51.100.42";
        ForgotPasswordRequest request = new ForgotPasswordRequest(tenant.getEmail());

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/forgot-password").with(fromIp(testIp))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        // 6th request from same IP must be rate limited
        mockMvc.perform(post("/api/v1/auth/forgot-password").with(fromIp(testIp))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    void rateLimit_resetPassword_exceedsFiveRequests_returns429() throws Exception {
        awaitFreshWindow();
        String testIp = "198.51.100.99";
        ResetPasswordRequest request = new ResetPasswordRequest("token-does-not-matter", "password123");

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/reset-password").with(fromIp(testIp))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest()); // bad token but passes rate limiter
        }

        // 6th request from same IP must be rate limited
        mockMvc.perform(post("/api/v1/auth/reset-password").with(fromIp(testIp))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    void unauthenticatedAccess_forgotAndResetPermitted_unrelatedEndpointsRemainProtected() throws Exception {
        // 1. forgot-password is reachable without authentication (200 OK)
        ForgotPasswordRequest forgotReq = new ForgotPasswordRequest(tenant.getEmail());
        mockMvc.perform(postApi("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotReq)))
                .andExpect(status().isOk());

        // 2. reset-password is reachable without authentication (400 Bad Request due to invalid token, NOT 401/403)
        ResetPasswordRequest resetReq = new ResetPasswordRequest("invalid-dummy-token", "newPass123");
        mockMvc.perform(postApi("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isBadRequest());

        // 3. Unrelated protected endpoint without auth -> 401 Unauthorized
        ChangePasswordRequest changeReq = new ChangePasswordRequest("old", "newPass123", "newPass123");
        mockMvc.perform(postApi("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeReq)))
                .andExpect(status().isUnauthorized());

        // 4. Other protected API without auth -> 401/403
        mockMvc.perform(get("/api/v1/tenants/me").with(uniqueIp()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usernameEmailCollision_betweenDifferentAccounts_suppressesResetToken() throws Exception {
        // Create a distinct tenant and staff account where staff email matches owner's username
        String suffix2 = UUID.randomUUID().toString().substring(0, 8);
        Tenant tenant2 = new Tenant();
        tenant2.setName("Spa Two " + suffix2);
        tenant2.setSlug("spa-two-" + suffix2);
        tenant2.setEmail("tenant2-" + suffix2 + "@example.com");
        tenant2.setIsActive(true);
        tenant2 = tenantRepository.save(tenant2);

        Staff collidingStaff = new Staff();
        collidingStaff.setTenant(tenant2);
        collidingStaff.setName("Colliding Staff");
        collidingStaff.setEmail(ownerUser.getUsername()); // Collision with ownerUser's username!
        collidingStaff.setIsActive(true);
        collidingStaff.setIsDeleted(false);
        collidingStaff = staffRepository.save(collidingStaff);

        User collidingStaffUser = new User();
        collidingStaffUser.setTenant(tenant2);
        collidingStaffUser.setUsername("staffuser-" + suffix2);
        collidingStaffUser.setPassword(passwordEncoder.encode("password123"));
        collidingStaffUser.setRole(UserRole.STAFF);
        collidingStaffUser.setStaff(collidingStaff);
        collidingStaffUser.setIsActive(true);
        collidingStaffUser.setTokenVersion(0);
        userRepository.save(collidingStaffUser);

        // Request reset for the colliding identifier
        ForgotPasswordRequest request = new ForgotPasswordRequest(ownerUser.getUsername());

        mockMvc.perform(postApi("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(PasswordResetService.GENERIC_FORGOT_SUCCESS_MESSAGE));

        // Delivery must be suppressed: 0 tokens generated for either user
        List<PasswordResetToken> tokens = tokenRepository.findAll()
                .stream()
                .filter(t -> t.getUser().getId().equals(ownerUser.getId()) || t.getUser().getId().equals(collidingStaffUser.getId()))
                .toList();
        assertThat(tokens).isEmpty();
    }

    @Test
    void legacyJwtWithoutTvClaim_worksBeforeReset_andIsRejectedAfterReset() throws Exception {
        // Mint a legacy JWT without the "tv" claim (simulates token minted prior to migration V16)
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        String legacyJwt = Jwts.builder()
                .subject(ownerUser.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(key)
                .compact();

        // 1. Before reset: user.tokenVersion == 0 -> legacy JWT (effective tv=0) MUST SUCCEED
        ChangePasswordRequest changeReq = new ChangePasswordRequest("oldPassword123", "tempPassword456", "tempPassword456");
        mockMvc.perform(postApi("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + legacyJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeReq)))
                .andExpect(status().isOk());

        // 2. Perform password reset -> user.tokenVersion increments to 1
        String rawToken = "legacy-jwt-test-token-" + UUID.randomUUID();
        tokenRepository.save(new PasswordResetToken(ownerUser, PasswordResetService.hashToken(rawToken), LocalDateTime.now().plusMinutes(15)));

        ResetPasswordRequest resetReq = new ResetPasswordRequest(rawToken, "brandNewPass789");
        mockMvc.perform(postApi("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isOk());

        // 3. After reset: legacy JWT (effective tv=0) MUST BE REJECTED (401 Unauthorized)
        mockMvc.perform(postApi("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + legacyJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeReq)))
                .andExpect(status().isUnauthorized());

        // 4. Newly issued JWT (contains tv=1) MUST SUCCEED
        LoginRequest loginReq = new LoginRequest(ownerUser.getUsername(), "brandNewPass789");
        String loginResponse = mockMvc.perform(postApi("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String newJwt = objectMapper.readTree(loginResponse).get("accessToken").asText();
        ChangePasswordRequest finalChange = new ChangePasswordRequest("brandNewPass789", "yetAnotherPass000", "yetAnotherPass000");
        mockMvc.perform(postApi("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + newJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(finalChange)))
                .andExpect(status().isOk());
    }
}
