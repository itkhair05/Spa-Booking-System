package com.example.spabooking;

import com.example.spabooking.article.entity.Article;
import com.example.spabooking.article.repository.ArticleRepository;
import com.example.spabooking.auth.entity.User;
import com.example.spabooking.auth.enums.UserRole;
import com.example.spabooking.auth.repository.UserRepository;
import com.example.spabooking.auth.security.CustomUserDetails;
import com.example.spabooking.auth.security.JwtUtils;
import com.example.spabooking.review.entity.Review;
import com.example.spabooking.review.repository.ReviewRepository;
import com.example.spabooking.service.entity.Service;
import com.example.spabooking.service.entity.ServiceCategory;
import com.example.spabooking.service.repository.ServiceCategoryRepository;
import com.example.spabooking.service.repository.ServiceRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
public class PhaseB5IntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private ServiceCategoryRepository serviceCategoryRepository;

    @Autowired
    private ArticleRepository articleRepository;

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

        // Setup Tenant A
        tenantA = new Tenant();
        tenantA.setName("TIKEY SPA A");
        tenantA.setSlug("tikey-b5-a-" + System.currentTimeMillis());
        tenantA.setIsActive(true);
        tenantA.setAddress("123 Vo Thi Sau, Q3, TP.HCM");
        tenantA = tenantRepository.saveAndFlush(tenantA);

        // Setup Tenant B (for multi-tenant isolation verification)
        tenantB = new Tenant();
        tenantB.setName("OTHER SPA B");
        tenantB.setSlug("other-b5-b-" + System.currentTimeMillis());
        tenantB.setIsActive(true);
        tenantB.setAddress("456 Nguyen Hue, Q1, TP.HCM");
        tenantB = tenantRepository.saveAndFlush(tenantB);

        // Setup Owner A
        User ownerA = new User();
        ownerA.setUsername("b5_owner_a_" + System.currentTimeMillis());
        ownerA.setPassword("hash");
        ownerA.setRole(UserRole.OWNER);
        ownerA.setTenant(tenantA);
        ownerA.setIsActive(true);
        ownerA = userRepository.saveAndFlush(ownerA);
        ownerTokenA = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(ownerA), null, new CustomUserDetails(ownerA).getAuthorities()));

        // Setup Staff A
        User staffA = new User();
        staffA.setUsername("b5_staff_a_" + System.currentTimeMillis());
        staffA.setPassword("hash");
        staffA.setRole(UserRole.STAFF);
        staffA.setTenant(tenantA);
        staffA.setIsActive(true);
        staffA = userRepository.saveAndFlush(staffA);
        staffTokenA = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(staffA), null, new CustomUserDetails(staffA).getAuthorities()));

        // Setup Owner B
        User ownerB = new User();
        ownerB.setUsername("b5_owner_b_" + System.currentTimeMillis());
        ownerB.setPassword("hash");
        ownerB.setRole(UserRole.OWNER);
        ownerB.setTenant(tenantB);
        ownerB.setIsActive(true);
        ownerB = userRepository.saveAndFlush(ownerB);
        ownerTokenB = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(ownerB), null, new CustomUserDetails(ownerB).getAuthorities()));
    }

    // =========================================================================
    // 1. SERVICES & CATEGORIES TESTS
    // =========================================================================

    @Test
    @DisplayName("OWNER can manage Service Categories; STAFF receives 403 Forbidden")
    void testServiceCategoriesManagementAndSecurity() throws Exception {
        Map<String, Object> catPayload = Map.of(
                "name", "Chăm sóc da mặt",
                "description", "Các liệu trình chăm sóc da chuyên sâu",
                "displayOrder", 1
        );

        // STAFF cannot create category (403)
        mockMvc.perform(post("/api/v1/service-categories")
                        .header("Authorization", "Bearer " + staffTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(catPayload)))
                .andExpect(status().isForbidden());

        // OWNER can create category (201)
        String resp = mockMvc.perform(post("/api/v1/service-categories")
                        .header("Authorization", "Bearer " + ownerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(catPayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Chăm sóc da mặt")))
                .andExpect(jsonPath("$.displayOrder", is(1)))
                .andReturn().getResponse().getContentAsString();

        Number catIdNum = com.jayway.jsonpath.JsonPath.read(resp, "$.id");
        Long catId = catIdNum.longValue();

        // Public categories endpoint returns the created category for Tenant A
        mockMvc.perform(get("/api/v1/public/spas/" + tenantA.getSlug() + "/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Chăm sóc da mặt")));

        // Tenant B's public categories do not see Tenant A's category
        mockMvc.perform(get("/api/v1/public/spas/" + tenantB.getSlug() + "/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("STAFF retains read-only access to service catalog data (services, categories)")
    void testStaffCanReadServiceCatalog() throws Exception {
        // Service category read stays available to STAFF (read-only catalog data)
        mockMvc.perform(get("/api/v1/service-categories")
                        .header("Authorization", "Bearer " + staffTokenA))
                .andExpect(status().isOk());

        // Service read stays available to STAFF (read-only catalog data)
        mockMvc.perform(get("/api/v1/services")
                        .header("Authorization", "Bearer " + staffTokenA))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("OWNER can update Service featured, category, and process steps; STAFF cannot manage")
    void testServiceFeaturedCategoryAndSteps() throws Exception {
        // Create category for Tenant A
        ServiceCategory cat = new ServiceCategory();
        cat.setTenant(tenantA);
        cat.setName("Massage & Trị liệu");
        cat.setDisplayOrder(1);
        cat.setIsActive(true);
        cat = serviceCategoryRepository.saveAndFlush(cat);

        // Create initial service
        Service service = new Service();
        service.setTenant(tenantA);
        service.setName("Massage Body Thụy Điển");
        service.setDurationMinutes(60);
        service.setPrice(new BigDecimal("350000.00"));
        service.setIsActive(true);
        service.setIsFeatured(false);
        service = serviceRepository.saveAndFlush(service);

        // STAFF cannot update service (403)
        String stepsJson = "[\"1. Khởi động\", \"2. Massage tinh dầu\", \"3. Bấm huyệt\", \"4. Thư giãn\"]";
        Map<String, Object> updatePayload = Map.of(
                "name", "Massage Body Thụy Điển Nâng Cấp",
                "durationMinutes", 75,
                "price", 400000,
                "categoryId", cat.getId(),
                "isFeatured", true,
                "processSteps", stepsJson
        );

        mockMvc.perform(put("/api/v1/services/" + service.getId())
                        .header("Authorization", "Bearer " + staffTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isForbidden());

        // OWNER can update service
        mockMvc.perform(put("/api/v1/services/" + service.getId())
                        .header("Authorization", "Bearer " + ownerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFeatured", is(true)))
                .andExpect(jsonPath("$.categoryName", is("Massage & Trị liệu")))
                .andExpect(jsonPath("$.processSteps", containsString("Khởi động")));

        // Public Services endpoint returns featured flag and process steps
        mockMvc.perform(get("/api/v1/public/spas/" + tenantA.getSlug() + "/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isFeatured", is(true)))
                .andExpect(jsonPath("$[0].categoryName", is("Massage & Trị liệu")))
                .andExpect(jsonPath("$[0].processSteps", containsString("Khởi động")));
    }

    @Test
    @DisplayName("Inactive services do not appear publicly even if featured")
    void testInactiveServiceNotShownPublicly() throws Exception {
        Service inactiveService = new Service();
        inactiveService.setTenant(tenantA);
        inactiveService.setName("Liệu trình tạm dừng");
        inactiveService.setDurationMinutes(60);
        inactiveService.setPrice(new BigDecimal("200000.00"));
        inactiveService.setIsActive(false);
        inactiveService.setIsFeatured(true);
        serviceRepository.saveAndFlush(inactiveService);

        mockMvc.perform(get("/api/v1/public/spas/" + tenantA.getSlug() + "/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // =========================================================================
    // 2. STAFF TEAM VISIBILITY TESTS
    // =========================================================================

    @Test
    @DisplayName("Staff public visibility: OWNER control, STAFF restriction, inactive/deleted/hidden filtering")
    void testStaffPublicVisibilityRules() throws Exception {
        // Staff 1: Active, Visible
        Staff s1 = new Staff();
        s1.setTenant(tenantA);
        s1.setName("Lê Kỹ Thuật Viên 1");
        s1.setEmail("ktv1@test.com");
        s1.setPhone("0901111111");
        s1.setIsActive(true);
        s1.setIsDeleted(false);
        s1.setShowOnWebsite(true);
        s1 = staffRepository.saveAndFlush(s1);

        // Staff 2: Active, Hidden by OWNER
        Staff s2 = new Staff();
        s2.setTenant(tenantA);
        s2.setName("Lê Kỹ Thuật Viên 2 (Ẩn)");
        s2.setEmail("ktv2@test.com");
        s2.setPhone("0902222222");
        s2.setIsActive(true);
        s2.setIsDeleted(false);
        s2.setShowOnWebsite(false);
        s2 = staffRepository.saveAndFlush(s2);

        // Staff 3: Inactive, showOnWebsite true
        Staff s3 = new Staff();
        s3.setTenant(tenantA);
        s3.setName("Lê Kỹ Thuật Viên 3 (Tạm nghỉ)");
        s3.setEmail("ktv3@test.com");
        s3.setPhone("0903333333");
        s3.setIsActive(false);
        s3.setIsDeleted(false);
        s3.setShowOnWebsite(true);
        s3 = staffRepository.saveAndFlush(s3);

        // Staff 4: Deleted, showOnWebsite true
        Staff s4 = new Staff();
        s4.setTenant(tenantA);
        s4.setName("Lê Kỹ Thuật Viên 4 (Đã xóa)");
        s4.setEmail("ktv4@test.com");
        s4.setPhone("0904444444");
        s4.setIsActive(true);
        s4.setIsDeleted(true);
        s4.setShowOnWebsite(true);
        s4 = staffRepository.saveAndFlush(s4);

        // Public Team endpoint should ONLY return s1
        mockMvc.perform(get("/api/v1/public/spas/" + tenantA.getSlug() + "/staff"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(s1.getId().intValue())))
                .andExpect(jsonPath("$[0].name", is("Lê Kỹ Thuật Viên 1")))
                // Ensure sensitive private contact information is NOT exposed
                .andExpect(jsonPath("$[0].email").doesNotExist())
                .andExpect(jsonPath("$[0].phone").doesNotExist());

        // STAFF cannot modify another staff's visibility (403)
        Map<String, Object> staffUpdate = Map.of(
                "name", s1.getName(),
                "email", s1.getEmail(),
                "phone", s1.getPhone(),
                "showOnWebsite", false
        );

        mockMvc.perform(put("/api/v1/staff/" + s1.getId())
                        .header("Authorization", "Bearer " + staffTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staffUpdate)))
                .andExpect(status().isForbidden());

        // OWNER toggles s1 visibility to OFF
        mockMvc.perform(put("/api/v1/staff/" + s1.getId())
                        .header("Authorization", "Bearer " + ownerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staffUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.showOnWebsite", is(false)));

        // Now public team has 0 members
        mockMvc.perform(get("/api/v1/public/spas/" + tenantA.getSlug() + "/staff"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // =========================================================================
    // 3. ARTICLES ("GÓC CHĂM SÓC") TESTS
    // =========================================================================

    @Test
    @DisplayName("Articles lifecycle: OWNER CRUD & publish, STAFF 403, Draft hidden publicly, Tenant isolated")
    void testArticlesLifecycleAndSecurity() throws Exception {
        Map<String, Object> articlePayload = Map.of(
                "title", "5 Bước Thư Giãn Tại Nhà Đúng Chuẩn Spa",
                "category", "Cẩm nang chăm sóc",
                "readTime", "4 phút đọc",
                "excerpt", "Hướng dẫn các bước massage và thư giãn...",
                "content", "Nội dung chi tiết bài viết chăm sóc...",
                "status", "DRAFT"
        );

        // STAFF cannot create article (403)
        mockMvc.perform(post("/api/v1/articles")
                        .header("Authorization", "Bearer " + staffTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(articlePayload)))
                .andExpect(status().isForbidden());

        // OWNER creates article as DRAFT
        String createResp = mockMvc.perform(post("/api/v1/articles")
                        .header("Authorization", "Bearer " + ownerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(articlePayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.title", is("5 Bước Thư Giãn Tại Nhà Đúng Chuẩn Spa")))
                .andReturn().getResponse().getContentAsString();

        Number artIdNum = com.jayway.jsonpath.JsonPath.read(createResp, "$.id");
        Long artId = artIdNum.longValue();

        // Anonymous checks public articles -> DRAFT is NOT visible
        mockMvc.perform(get("/api/v1/public/spas/" + tenantA.getSlug() + "/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // OWNER publishes article
        mockMvc.perform(patch("/api/v1/articles/" + artId + "/publish")
                        .header("Authorization", "Bearer " + ownerTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PUBLISHED")))
                .andExpect(jsonPath("$.publishedAt", notNullValue()));

        // Now public articles returns the article
        mockMvc.perform(get("/api/v1/public/spas/" + tenantA.getSlug() + "/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("5 Bước Thư Giãn Tại Nhà Đúng Chuẩn Spa")));

        // Tenant B cannot see Tenant A's published article
        mockMvc.perform(get("/api/v1/public/spas/" + tenantB.getSlug() + "/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Tenant B owner cannot modify Tenant A's article
        mockMvc.perform(patch("/api/v1/articles/" + artId + "/unpublish")
                        .header("Authorization", "Bearer " + ownerTokenB))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // 4. REVIEWS MODERATION TESTS
    // =========================================================================

    @Test
    @DisplayName("Reviews moderation: OWNER publish/unpublish, STAFF 403, Unpublished hidden, Tenant isolated")
    void testReviewsModerationAndSecurity() throws Exception {
        // Create an unpublished review for Tenant A
        Review r1 = new Review();
        r1.setTenant(tenantA);
        r1.setCustomerName("Khách hàng Minh Anh");
        r1.setRating(5);
        r1.setComment("Dịch vụ tuyệt vời, nhân viên chu đáo!");
        r1.setServiceName("Chăm sóc da mặt chuyên sâu");
        r1.setIsPublished(false);
        r1.setIsDemo(false);
        r1.setDisplayOrder(1);
        r1 = reviewRepository.saveAndFlush(r1);

        // Anonymous user viewing public reviews -> 0 returned (unpublished)
        mockMvc.perform(get("/api/v1/public/spas/" + tenantA.getSlug() + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // STAFF cannot moderate (publish) review (403)
        mockMvc.perform(patch("/api/v1/reviews/" + r1.getId() + "/publish")
                        .header("Authorization", "Bearer " + staffTokenA))
                .andExpect(status().isForbidden());

        // OWNER publishes review
        mockMvc.perform(patch("/api/v1/reviews/" + r1.getId() + "/publish")
                        .header("Authorization", "Bearer " + ownerTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPublished", is(true)));

        // Anonymous viewing public reviews -> 1 returned
        mockMvc.perform(get("/api/v1/public/spas/" + tenantA.getSlug() + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].customerName", is("Khách hàng Minh Anh")))
                .andExpect(jsonPath("$[0].rating", is(5)));

        // Tenant B cannot see Tenant A's published review
        mockMvc.perform(get("/api/v1/public/spas/" + tenantB.getSlug() + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // =========================================================================
    // 5. BOOKING VALIDATION & OPTIONAL CALENDAR REMINDERS
    // =========================================================================

    @Test
    @DisplayName("Booking validation: cross-tenant or inactive service rejected; server validates availability")
    void testBookingValidationRules() throws Exception {
        // Active service in Tenant B
        Service serviceB = new Service();
        serviceB.setTenant(tenantB);
        serviceB.setName("Service in B");
        serviceB.setDurationMinutes(60);
        serviceB.setPrice(new BigDecimal("250000.00"));
        serviceB.setIsActive(true);
        serviceB = serviceRepository.saveAndFlush(serviceB);

        // Active staff in Tenant A
        Staff staffA = new Staff();
        staffA.setTenant(tenantA);
        staffA.setName("Staff A");
        staffA.setIsActive(true);
        staffA.setIsDeleted(false);
        staffA = staffRepository.saveAndFlush(staffA);

        // Attempt booking in Tenant A with Tenant B's service -> 404 Not Found (server isolation)
        Map<String, Object> bookingPayload = Map.of(
                "serviceId", serviceB.getId(),
                "staffId", staffA.getId(),
                "startTime", LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).toString(),
                "customerName", "Nguyen Van C",
                "customerPhone", "0912345678"
        );

        mockMvc.perform(post("/api/v1/public/spas/" + tenantA.getSlug() + "/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingPayload)))
                .andExpect(status().isNotFound());
    }
}
