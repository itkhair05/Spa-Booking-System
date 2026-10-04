package com.example.spabooking.feedback;

import com.example.spabooking.auth.entity.User;
import com.example.spabooking.auth.enums.UserRole;
import com.example.spabooking.auth.repository.UserRepository;
import com.example.spabooking.auth.security.CustomUserDetails;
import com.example.spabooking.auth.security.JwtUtils;
import com.example.spabooking.feedback.dto.CreateFeedbackRequest;
import com.example.spabooking.feedback.dto.UpdateFeedbackStatusRequest;
import com.example.spabooking.feedback.entity.Feedback;
import com.example.spabooking.feedback.entity.FeedbackStatus;
import com.example.spabooking.feedback.entity.FeedbackType;
import com.example.spabooking.feedback.repository.FeedbackRepository;
import com.example.spabooking.staff.entity.Staff;
import com.example.spabooking.staff.repository.StaffRepository;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class FeedbackSecurityTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StaffRepository staffRepository;
    @Autowired
    private FeedbackRepository feedbackRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtils jwtUtils;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    private Tenant tenantA;
    private Tenant tenantB;
    private String ownerTokenA;
    private String staffTokenA;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        tenantA = new Tenant();
        tenantA.setName("TIKEY SPA A");
        tenantA.setSlug("tikey-spa-a-" + UUID.randomUUID());
        tenantA.setTimezone("Asia/Ho_Chi_Minh");
        tenantA = tenantRepository.saveAndFlush(tenantA);

        tenantB = new Tenant();
        tenantB.setName("TIKEY SPA B");
        tenantB.setSlug("tikey-spa-b-" + UUID.randomUUID());
        tenantB.setTimezone("Asia/Ho_Chi_Minh");
        tenantB = tenantRepository.saveAndFlush(tenantB);

        User ownerA = new User();
        ownerA.setTenant(tenantA);
        ownerA.setUsername("owner_a_" + UUID.randomUUID());
        ownerA.setPassword(passwordEncoder.encode("password"));
        ownerA.setRole(UserRole.OWNER);
        ownerA = userRepository.saveAndFlush(ownerA);
        ownerTokenA = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(ownerA), null, new CustomUserDetails(ownerA).getAuthorities()));

        Staff staffA = new Staff();
        staffA.setName("Staff A");
        staffA.setTenant(tenantA);
        staffA.setIsActive(true);
        staffA = staffRepository.saveAndFlush(staffA);

        User staffUserA = new User();
        staffUserA.setTenant(tenantA);
        staffUserA.setUsername("staff_a_" + UUID.randomUUID());
        staffUserA.setPassword(passwordEncoder.encode("password"));
        staffUserA.setRole(UserRole.STAFF);
        staffUserA.setStaff(staffA);
        staffUserA = userRepository.saveAndFlush(staffUserA);
        staffTokenA = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(staffUserA), null, new CustomUserDetails(staffUserA).getAuthorities()));
    }

    @Test
    @DisplayName("Public customer can submit valid feedback anonymously")
    void testPublicSubmitFeedback_Success() throws Exception {
        CreateFeedbackRequest request = new CreateFeedbackRequest(
                "Trần Văn B",
                "0912345678",
                "tranvanb@example.com",
                FeedbackType.COMPLAINT,
                "BK-123456",
                "Dịch vụ massage hôm nay nhân viên đến hơi trễ 10 phút."
        );

        mockMvc.perform(post("/api/v1/public/spas/" + tenantA.getSlug() + "/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Trần Văn B"))
                .andExpect(jsonPath("$.phone").value("0912345678"))
                .andExpect(jsonPath("$.type").value("COMPLAINT"))
                .andExpect(jsonPath("$.bookingCode").value("BK-123456"))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    @DisplayName("Public submit feedback with missing required fields returns 400 Bad Request")
    void testPublicSubmitFeedback_InvalidRequest() throws Exception {
        CreateFeedbackRequest request = new CreateFeedbackRequest();
        request.setName(""); // blank
        request.setPhone(""); // blank
        request.setMessage(""); // blank

        mockMvc.perform(post("/api/v1/public/spas/" + tenantA.getSlug() + "/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Anonymous request to OWNER feedback management returns 401 Unauthorized")
    void testGetFeedback_Anonymous_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/feedback"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("STAFF request to OWNER feedback management returns 403 Forbidden")
    void testGetFeedback_Staff_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/feedback")
                        .header("Authorization", "Bearer " + staffTokenA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("OWNER can view all feedback for their tenant")
    void testGetFeedback_Owner_Success() throws Exception {
        Feedback fb = new Feedback();
        fb.setTenant(tenantA);
        fb.setName("Khách Hàng A");
        fb.setPhone("0987654321");
        fb.setType(FeedbackType.PRAISE);
        fb.setMessage("Dịch vụ tuyệt vời!");
        fb.setStatus(FeedbackStatus.NEW);
        feedbackRepository.saveAndFlush(fb);

        mockMvc.perform(get("/api/v1/feedback")
                        .header("Authorization", "Bearer " + ownerTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Khách Hàng A"))
                .andExpect(jsonPath("$[0].type").value("PRAISE"));
    }

    @Test
    @DisplayName("OWNER can update feedback status")
    void testUpdateFeedbackStatus_Owner_Success() throws Exception {
        Feedback fb = new Feedback();
        fb.setTenant(tenantA);
        fb.setName("Khách Hàng B");
        fb.setPhone("0987654322");
        fb.setType(FeedbackType.SUGGESTION);
        fb.setMessage("Nên thêm mùi tinh dầu sả chanh.");
        fb.setStatus(FeedbackStatus.NEW);
        fb = feedbackRepository.saveAndFlush(fb);

        UpdateFeedbackStatusRequest updateReq = new UpdateFeedbackStatusRequest(FeedbackStatus.RESOLVED);

        mockMvc.perform(patch("/api/v1/feedback/" + fb.getId() + "/status")
                        .header("Authorization", "Bearer " + ownerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(fb.getId()))
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    @DisplayName("Cross-tenant isolation: Tenant B cannot see Tenant A's feedback")
    void testCrossTenantIsolation() throws Exception {
        Feedback fbA = new Feedback();
        fbA.setTenant(tenantA);
        fbA.setName("Feedback Tenant A");
        fbA.setPhone("0900000001");
        fbA.setType(FeedbackType.OTHER);
        fbA.setMessage("Message A");
        feedbackRepository.saveAndFlush(fbA);

        User ownerB = new User();
        ownerB.setTenant(tenantB);
        ownerB.setUsername("owner_b_" + UUID.randomUUID());
        ownerB.setPassword(passwordEncoder.encode("password"));
        ownerB.setRole(UserRole.OWNER);
        ownerB = userRepository.saveAndFlush(ownerB);
        String ownerTokenB = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(ownerB), null, new CustomUserDetails(ownerB).getAuthorities()));

        mockMvc.perform(get("/api/v1/feedback")
                        .header("Authorization", "Bearer " + ownerTokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
