package com.example.spabooking.review;

import com.example.spabooking.auth.entity.User;
import com.example.spabooking.auth.enums.UserRole;
import com.example.spabooking.auth.repository.UserRepository;
import com.example.spabooking.auth.security.CustomUserDetails;
import com.example.spabooking.auth.security.JwtUtils;
import com.example.spabooking.review.dto.CreateReviewRequest;
import com.example.spabooking.review.dto.UpdateReviewRequest;
import com.example.spabooking.review.entity.Review;
import com.example.spabooking.review.repository.ReviewRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
public class ReviewManagementIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private JwtUtils jwtUtils;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Tenant tenantA;
    private Tenant tenantB;

    private String ownerTokenA;
    private String staffTokenA;
    private String ownerTokenB;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        long now = System.currentTimeMillis();

        tenantA = new Tenant();
        tenantA.setName("TIKEY SPA A");
        tenantA.setSlug("tikey-rev-a-" + now);
        tenantA.setIsActive(true);
        tenantA = tenantRepository.saveAndFlush(tenantA);

        tenantB = new Tenant();
        tenantB.setName("OTHER SPA B");
        tenantB.setSlug("other-rev-b-" + now);
        tenantB.setIsActive(true);
        tenantB = tenantRepository.saveAndFlush(tenantB);

        User ownerA = new User();
        ownerA.setUsername("rev_owner_a_" + now);
        ownerA.setPassword("hash");
        ownerA.setRole(UserRole.OWNER);
        ownerA.setTenant(tenantA);
        ownerA.setIsActive(true);
        ownerA = userRepository.saveAndFlush(ownerA);
        ownerTokenA = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(ownerA), null, new CustomUserDetails(ownerA).getAuthorities()));

        User staffA = new User();
        staffA.setUsername("rev_staff_a_" + now);
        staffA.setPassword("hash");
        staffA.setRole(UserRole.STAFF);
        staffA.setTenant(tenantA);
        staffA.setIsActive(true);
        staffA = userRepository.saveAndFlush(staffA);
        staffTokenA = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(staffA), null, new CustomUserDetails(staffA).getAuthorities()));

        User ownerB = new User();
        ownerB.setUsername("rev_owner_b_" + now);
        ownerB.setPassword("hash");
        ownerB.setRole(UserRole.OWNER);
        ownerB.setTenant(tenantB);
        ownerB.setIsActive(true);
        ownerB = userRepository.saveAndFlush(ownerB);
        ownerTokenB = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(ownerB), null, new CustomUserDetails(ownerB).getAuthorities()));
    }

    @Test
    @DisplayName("OWNER can create a review with valid fields")
    void testOwnerCanCreateReview() throws Exception {
        CreateReviewRequest req = new CreateReviewRequest();
        req.setCustomerName("Chị Lan Phương");
        req.setRating(5);
        req.setComment("Liệu trình chăm sóc da mặt rất dịu nhẹ, da căng mịn ngay sau buổi đầu!");
        req.setServiceName("Chăm sóc da mặt chuyên sâu");
        req.setDisplayOrder(1);
        req.setIsPublished(true);
        req.setIsDemo(false);

        mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", "Bearer " + ownerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.customerName", is("Chị Lan Phương")))
                .andExpect(jsonPath("$.rating", is(5)))
                .andExpect(jsonPath("$.comment", containsString("Liệu trình chăm sóc")))
                .andExpect(jsonPath("$.serviceName", is("Chăm sóc da mặt chuyên sâu")))
                .andExpect(jsonPath("$.isPublished", is(true)))
                .andExpect(jsonPath("$.isDemo", is(false)))
                .andExpect(jsonPath("$.displayOrder", is(1)));
    }

    @Test
    @DisplayName("Create review fails validation if customerName or comment is blank, or rating out of bounds")
    void testCreateReviewValidation() throws Exception {
        // Blank customer name
        CreateReviewRequest req1 = new CreateReviewRequest();
        req1.setCustomerName("   ");
        req1.setRating(5);
        req1.setComment("Dịch vụ tốt");

        mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", "Bearer " + ownerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isBadRequest());

        // Blank comment
        CreateReviewRequest req2 = new CreateReviewRequest();
        req2.setCustomerName("Khách hàng");
        req2.setRating(5);
        req2.setComment("   ");

        mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", "Bearer " + ownerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isBadRequest());

        // Rating > 5
        CreateReviewRequest req3 = new CreateReviewRequest();
        req3.setCustomerName("Khách hàng");
        req3.setRating(6);
        req3.setComment("Dịch vụ tốt");

        mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", "Bearer " + ownerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req3)))
                .andExpect(status().isBadRequest());

        // Rating < 1
        CreateReviewRequest req4 = new CreateReviewRequest();
        req4.setCustomerName("Khách hàng");
        req4.setRating(0);
        req4.setComment("Dịch vụ tốt");

        mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", "Bearer " + ownerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req4)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("OWNER can update an existing review")
    void testOwnerCanUpdateReview() throws Exception {
        Review review = new Review();
        review.setTenant(tenantA);
        review.setCustomerName("Tên Ban Đầu");
        review.setRating(5);
        review.setComment("Nhận xét ban đầu");
        review.setServiceName("Dịch vụ cũ");
        review.setIsPublished(false);
        review.setIsDemo(false);
        review.setDisplayOrder(0);
        review = reviewRepository.saveAndFlush(review);

        UpdateReviewRequest updateReq = new UpdateReviewRequest();
        updateReq.setCustomerName("Tên Đã Đổi");
        updateReq.setRating(4);
        updateReq.setComment("Nhận xét sau khi chỉnh sửa");
        updateReq.setServiceName("Dịch vụ mới");
        updateReq.setIsPublished(true);
        updateReq.setIsDemo(true);
        updateReq.setDisplayOrder(5);

        mockMvc.perform(put("/api/v1/reviews/" + review.getId())
                        .header("Authorization", "Bearer " + ownerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName", is("Tên Đã Đổi")))
                .andExpect(jsonPath("$.rating", is(4)))
                .andExpect(jsonPath("$.comment", is("Nhận xét sau khi chỉnh sửa")))
                .andExpect(jsonPath("$.serviceName", is("Dịch vụ mới")))
                .andExpect(jsonPath("$.isPublished", is(true)))
                .andExpect(jsonPath("$.isDemo", is(true)))
                .andExpect(jsonPath("$.displayOrder", is(5)));
    }

    @Test
    @DisplayName("OWNER can delete a review")
    void testOwnerCanDeleteReview() throws Exception {
        Review review = new Review();
        review.setTenant(tenantA);
        review.setCustomerName("Cần Xóa");
        review.setRating(5);
        review.setComment("Nội dung cần xóa");
        review.setIsPublished(true);
        review = reviewRepository.saveAndFlush(review);

        mockMvc.perform(delete("/api/v1/reviews/" + review.getId())
                        .header("Authorization", "Bearer " + ownerTokenA))
                .andExpect(status().isNoContent());

        assertFalse(reviewRepository.findById(review.getId()).isPresent());
    }

    @Test
    @DisplayName("STAFF and unauthenticated users are forbidden from managing reviews")
    void testStaffAndUnauthenticatedForbidden() throws Exception {
        Review review = new Review();
        review.setTenant(tenantA);
        review.setCustomerName("Bảo Mật");
        review.setRating(5);
        review.setComment("Kiểm tra quyền truy cập");
        review = reviewRepository.saveAndFlush(review);

        CreateReviewRequest req = new CreateReviewRequest();
        req.setCustomerName("Khách Staff");
        req.setRating(5);
        req.setComment("Thử tạo bằng STAFF");

        // STAFF cannot GET reviews
        mockMvc.perform(get("/api/v1/reviews")
                        .header("Authorization", "Bearer " + staffTokenA))
                .andExpect(status().isForbidden());

        // STAFF cannot POST review
        mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", "Bearer " + staffTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        // STAFF cannot PUT review
        mockMvc.perform(put("/api/v1/reviews/" + review.getId())
                        .header("Authorization", "Bearer " + staffTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        // STAFF cannot DELETE review
        mockMvc.perform(delete("/api/v1/reviews/" + review.getId())
                        .header("Authorization", "Bearer " + staffTokenA))
                .andExpect(status().isForbidden());

        // Unauthenticated cannot access
        mockMvc.perform(get("/api/v1/reviews"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Tenant isolation: OWNER B cannot view, update, or delete Tenant A reviews")
    void testTenantIsolation() throws Exception {
        Review reviewA = new Review();
        reviewA.setTenant(tenantA);
        reviewA.setCustomerName("Khách Tenant A");
        reviewA.setRating(5);
        reviewA.setComment("Đánh giá của Tenant A");
        reviewA.setIsPublished(true);
        reviewA = reviewRepository.saveAndFlush(reviewA);

        // Owner B GET /reviews -> does not see reviewA
        mockMvc.perform(get("/api/v1/reviews")
                        .header("Authorization", "Bearer " + ownerTokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Owner B GET /reviews/{id} -> 404
        mockMvc.perform(get("/api/v1/reviews/" + reviewA.getId())
                        .header("Authorization", "Bearer " + ownerTokenB))
                .andExpect(status().isNotFound());

        // Owner B PUT /reviews/{id} -> 404
        UpdateReviewRequest updateReq = new UpdateReviewRequest();
        updateReq.setCustomerName("Hack");
        mockMvc.perform(put("/api/v1/reviews/" + reviewA.getId())
                        .header("Authorization", "Bearer " + ownerTokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());

        // Owner B DELETE /reviews/{id} -> 404
        mockMvc.perform(delete("/api/v1/reviews/" + reviewA.getId())
                        .header("Authorization", "Bearer " + ownerTokenB))
                .andExpect(status().isNotFound());
    }
}
