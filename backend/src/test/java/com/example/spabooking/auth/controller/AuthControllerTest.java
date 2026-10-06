package com.example.spabooking.auth.controller;

import com.example.spabooking.auth.dto.LoginRequest;
import com.example.spabooking.auth.entity.User;
import com.example.spabooking.auth.enums.UserRole;
import com.example.spabooking.auth.repository.UserRepository;
import com.example.spabooking.auth.security.JwtUtils;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class AuthControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        userRepository.findByUsername("testowner").ifPresent(userRepository::delete);
        tenantRepository.findBySlug("testspa").ifPresent(tenantRepository::delete);
        userRepository.flush();
        tenantRepository.flush();

        Tenant tenant = new Tenant();
        tenant.setName("Test Spa");
        tenant.setSlug("testspa");
        tenant.setIsActive(true);
        tenantRepository.save(tenant);

        User user = new User();
        user.setUsername("testowner");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(UserRole.OWNER);
        user.setIsActive(true);
        user.setTenant(tenant);
        userRepository.save(user);
    }

    @Test
    void testLogin_Success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("testowner");
        request.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value("testowner"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_OWNER"));
    }

    @Test
    void testLogin_InvalidPassword_ShouldReturn401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("testowner");
        request.setPassword("wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testLogin_NonexistentUsername_ShouldReturn401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("nonexistent");
        request.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testLogin_BlankUsername_ShouldReturn400() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("");
        request.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testProtectedEndpoint_WithoutJwt_ShouldReturn401() throws Exception {
        // Any other endpoint requires auth. Let's use a dummy one.
        mockMvc.perform(get("/api/v1/some-protected-endpoint"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testProtectedEndpoint_WithValidJwt_ShouldReturn404() throws Exception {
        // It should pass security (401) and reach dispatcher (404 since it doesn't exist)
        LoginRequest request = new LoginRequest();
        request.setUsername("testowner");
        request.setPassword("password123");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        String responseString = result.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseString).get("accessToken").asText();

        mockMvc.perform(get("/api/v1/some-protected-endpoint")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void testProtectedEndpoint_WithInvalidJwt_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/some-protected-endpoint")
                .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testProtectedEndpoint_WithTamperedJwt_ShouldReturn401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("testowner");
        request.setPassword("password123");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();

        // Flip the first character of the payload segment, keeping the original signature
        String[] parts = token.split("\\.");
        char original = parts[1].charAt(0);
        char different = original == 'A' ? 'B' : 'A';
        String tamperedToken = parts[0] + "." + different + parts[1].substring(1) + "." + parts[2];

        mockMvc.perform(get("/api/v1/some-protected-endpoint")
                .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized());
    }
}
