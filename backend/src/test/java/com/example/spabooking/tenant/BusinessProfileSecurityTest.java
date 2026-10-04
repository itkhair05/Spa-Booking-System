package com.example.spabooking.tenant;

import com.example.spabooking.auth.entity.User;
import com.example.spabooking.auth.enums.UserRole;
import com.example.spabooking.auth.repository.UserRepository;
import com.example.spabooking.auth.security.CustomUserDetails;
import com.example.spabooking.auth.security.JwtUtils;
import com.example.spabooking.tenant.dto.UpdateBusinessProfileRequest;
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

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class BusinessProfileSecurityTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Tenant tenantA;
    private Tenant tenantB;
    private String ownerAJwt;
    private String staffAJwt;
    private String ownerBJwt;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        tenantA = new Tenant();
        tenantA.setName("TIKEY SPA HCM");
        tenantA.setSlug("tikey-spa-hcm-" + System.currentTimeMillis());
        tenantA.setPhone("0901111222");
        tenantA.setEmail("hcm@tikeyspa.vn");
        tenantA.setAddress("123 Lê Lợi, Q1, TP.HCM");
        tenantA.setTimezone("Asia/Ho_Chi_Minh");
        tenantA = tenantRepository.saveAndFlush(tenantA);

        tenantB = new Tenant();
        tenantB.setName("Other Spa HN");
        tenantB.setSlug("other-spa-hn-" + System.currentTimeMillis());
        tenantB.setPhone("0903333444");
        tenantB = tenantRepository.saveAndFlush(tenantB);

        User ownerA = new User();
        ownerA.setUsername("ownerA_prof_" + System.currentTimeMillis());
        ownerA.setPassword("hash");
        ownerA.setRole(UserRole.OWNER);
        ownerA.setTenant(tenantA);
        ownerA.setIsActive(true);
        ownerA = userRepository.saveAndFlush(ownerA);

        User staffA = new User();
        staffA.setUsername("staffA_prof_" + System.currentTimeMillis());
        staffA.setPassword("hash");
        staffA.setRole(UserRole.STAFF);
        staffA.setTenant(tenantA);
        staffA.setIsActive(true);
        staffA = userRepository.saveAndFlush(staffA);

        User ownerB = new User();
        ownerB.setUsername("ownerB_prof_" + System.currentTimeMillis());
        ownerB.setPassword("hash");
        ownerB.setRole(UserRole.OWNER);
        ownerB.setTenant(tenantB);
        ownerB.setIsActive(true);
        ownerB = userRepository.saveAndFlush(ownerB);

        CustomUserDetails ownerADetails = new CustomUserDetails(ownerA);
        ownerAJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(ownerADetails, null, ownerADetails.getAuthorities()));

        CustomUserDetails staffADetails = new CustomUserDetails(staffA);
        staffAJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(staffADetails, null, staffADetails.getAuthorities()));

        CustomUserDetails ownerBDetails = new CustomUserDetails(ownerB);
        ownerBJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(ownerBDetails, null, ownerBDetails.getAuthorities()));
    }

    @Test
    void ownerCanViewBusinessProfile() throws Exception {
        mockMvc.perform(get("/api/v1/business-profile")
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("TIKEY SPA HCM")))
                .andExpect(jsonPath("$.phone", is("0901111222")))
                .andExpect(jsonPath("$.email", is("hcm@tikeyspa.vn")))
                .andExpect(jsonPath("$.address", is("123 Lê Lợi, Q1, TP.HCM")));
    }

    @Test
    void staffCanViewBusinessProfile() throws Exception {
        mockMvc.perform(get("/api/v1/business-profile")
                        .header("Authorization", "Bearer " + staffAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("TIKEY SPA HCM")));
    }

    @Test
    void ownerCanUpdateBusinessProfile() throws Exception {
        UpdateBusinessProfileRequest request = new UpdateBusinessProfileRequest();
        request.setName("TIKEY SPA Flagship");
        request.setPhone("0909999888");
        request.setEmail("contact@tikeyspa.vn");
        request.setAddress("456 Nguyễn Huệ, Q1, TP.HCM");

        mockMvc.perform(put("/api/v1/business-profile")
                        .header("Authorization", "Bearer " + ownerAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("TIKEY SPA Flagship")))
                .andExpect(jsonPath("$.phone", is("0909999888")))
                .andExpect(jsonPath("$.address", is("456 Nguyễn Huệ, Q1, TP.HCM")));
    }

    @Test
    void staffCannotUpdateBusinessProfile() throws Exception {
        UpdateBusinessProfileRequest request = new UpdateBusinessProfileRequest();
        request.setName("Hacked Spa Name");

        mockMvc.perform(put("/api/v1/business-profile")
                        .header("Authorization", "Bearer " + staffAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void businessProfileRespectsTenantIsolation() throws Exception {
        mockMvc.perform(get("/api/v1/business-profile")
                        .header("Authorization", "Bearer " + ownerBJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Other Spa HN")));
    }
}
