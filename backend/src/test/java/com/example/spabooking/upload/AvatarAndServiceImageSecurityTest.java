package com.example.spabooking.upload;

import com.example.spabooking.article.entity.Article;
import com.example.spabooking.article.repository.ArticleRepository;
import com.example.spabooking.auth.entity.User;
import com.example.spabooking.auth.enums.UserRole;
import com.example.spabooking.auth.repository.UserRepository;
import com.example.spabooking.auth.security.CustomUserDetails;
import com.example.spabooking.auth.security.JwtUtils;
import com.example.spabooking.service.entity.Service;
import com.example.spabooking.service.repository.ServiceRepository;
import com.example.spabooking.staff.entity.Staff;
import com.example.spabooking.staff.repository.StaffRepository;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AvatarAndServiceImageSecurityTest {

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
    private ArticleRepository articleRepository;
    @Autowired
    private JwtUtils jwtUtils;

    private MockMvc mockMvc;

    private Tenant tenant;
    private Staff staff1;
    private Staff staff2;
    private Service service;
    private Article article;

    private String ownerJwt;
    private String staff1Jwt;

    // Valid PNG header: 89 50 4E 47 0D 0A 1A 0A
    private final byte[] VALID_PNG_BYTES = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D};

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        tenant = new Tenant();
        tenant.setName("Upload Test Spa");
        tenant.setSlug("upload-spa-" + UUID.randomUUID());
        tenant = tenantRepository.saveAndFlush(tenant);

        User owner = new User();
        owner.setUsername("owner_up_" + UUID.randomUUID());
        owner.setPassword("encoded");
        owner.setRole(UserRole.OWNER);
        owner.setTenant(tenant);
        owner = userRepository.saveAndFlush(owner);

        staff1 = new Staff();
        staff1.setName("Staff One");
        staff1.setTenant(tenant);
        staff1.setIsActive(true);
        staff1 = staffRepository.saveAndFlush(staff1);

        staff2 = new Staff();
        staff2.setName("Staff Two");
        staff2.setTenant(tenant);
        staff2.setIsActive(true);
        staff2 = staffRepository.saveAndFlush(staff2);

        User staffUser = new User();
        staffUser.setUsername("staff_up_" + UUID.randomUUID());
        staffUser.setPassword("encoded");
        staffUser.setRole(UserRole.STAFF);
        staffUser.setTenant(tenant);
        staffUser.setStaff(staff1);
        staffUser = userRepository.saveAndFlush(staffUser);

        service = new Service();
        service.setName("Facial Treatment");
        service.setPrice(new BigDecimal("350000.00"));
        service.setDurationMinutes(60);
        service.setTenant(tenant);
        service.setIsActive(true);
        service = serviceRepository.saveAndFlush(service);

        article = new Article();
        article.setTenant(tenant);
        article.setTitle("Bài viết kiểm thử ảnh bìa");
        article.setSlug("bai-viet-test-" + UUID.randomUUID());
        article.setContent("Nội dung bài viết kiểm thử");
        article = articleRepository.saveAndFlush(article);

        ownerJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(owner), null, new CustomUserDetails(owner).getAuthorities()));
        staff1Jwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(staffUser), null, new CustomUserDetails(staffUser).getAuthorities()));
    }

    @Test
    void testStaffCanUploadOwnAvatarAndDownloadIt() throws Exception {
        MockMultipartFile validImage = new MockMultipartFile(
                "file", "avatar.png", "image/png", VALID_PNG_BYTES
        );

        String responseString = mockMvc.perform(multipart("/api/v1/staff/me/avatar")
                        .file(validImage)
                        .header("Authorization", "Bearer " + staff1Jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl", containsString("/api/v1/uploads/avatars/")))
                .andReturn().getResponse().getContentAsString();

        // Extract avatar URL
        String avatarUrl = responseString.split("\"avatarUrl\":\"")[1].split("\"")[0];

        // Public download of avatar should succeed
        mockMvc.perform(get(avatarUrl))
                .andExpect(status().isOk());
    }

    @Test
    void testStaffCannotUploadAvatarForAnotherStaff() throws Exception {
        MockMultipartFile validImage = new MockMultipartFile(
                "file", "avatar.png", "image/png", VALID_PNG_BYTES
        );

        mockMvc.perform(multipart("/api/v1/staff/" + staff2.getId() + "/avatar")
                        .file(validImage)
                        .header("Authorization", "Bearer " + staff1Jwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void testOwnerCanUploadStaffAvatar() throws Exception {
        MockMultipartFile validImage = new MockMultipartFile(
                "file", "avatar.png", "image/png", VALID_PNG_BYTES
        );

        mockMvc.perform(multipart("/api/v1/staff/" + staff2.getId() + "/avatar")
                        .file(validImage)
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl", containsString("/api/v1/uploads/avatars/")));
    }

    @Test
    void testOwnerCanUploadServiceImage() throws Exception {
        MockMultipartFile validImage = new MockMultipartFile(
                "file", "service.png", "image/png", VALID_PNG_BYTES
        );

        mockMvc.perform(multipart("/api/v1/services/" + service.getId() + "/image")
                        .file(validImage)
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl", containsString("/api/v1/uploads/services/")));
    }

    @Test
    void testStaffCannotUploadServiceImage() throws Exception {
        MockMultipartFile validImage = new MockMultipartFile(
                "file", "service.png", "image/png", VALID_PNG_BYTES
        );

        mockMvc.perform(multipart("/api/v1/services/" + service.getId() + "/image")
                        .file(validImage)
                        .header("Authorization", "Bearer " + staff1Jwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void testFakeExecutableDisguisedAsImageRejected() throws Exception {
        byte[] fakeExe = "MZ executable content".getBytes();
        MockMultipartFile badFile = new MockMultipartFile(
                "file", "virus.png", "image/png", fakeExe
        );

        mockMvc.perform(multipart("/api/v1/staff/me/avatar")
                        .file(badFile)
                        .header("Authorization", "Bearer " + staff1Jwt))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Chỉ chấp nhận tệp hình ảnh định dạng JPEG, PNG hoặc WEBP")));
    }

    @Test
    void testPathTraversalRejected() throws Exception {
        mockMvc.perform(get("/api/v1/uploads/avatars/..%2F..%2Fsecret.txt"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void testOwnerCanUploadArticleCoverAndDownloadIt() throws Exception {
        MockMultipartFile validImage = new MockMultipartFile(
                "file", "cover.png", "image/png", VALID_PNG_BYTES
        );

        String responseString = mockMvc.perform(multipart("/api/v1/articles/" + article.getId() + "/cover")
                        .file(validImage)
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverImage", containsString("/api/v1/uploads/articles/")))
                .andReturn().getResponse().getContentAsString();

        String coverUrl = responseString.split("\"coverImage\":\"")[1].split("\"")[0];

        mockMvc.perform(get(coverUrl))
                .andExpect(status().isOk());
    }

    @Test
    void testStaffCannotUploadArticleCover() throws Exception {
        MockMultipartFile validImage = new MockMultipartFile(
                "file", "cover.png", "image/png", VALID_PNG_BYTES
        );

        mockMvc.perform(multipart("/api/v1/articles/" + article.getId() + "/cover")
                        .file(validImage)
                        .header("Authorization", "Bearer " + staff1Jwt))
                .andExpect(status().isForbidden());
    }
}
