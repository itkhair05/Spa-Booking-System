package com.example.spabooking.staff;

import com.example.spabooking.auth.entity.User;
import com.example.spabooking.auth.enums.UserRole;
import com.example.spabooking.auth.repository.UserRepository;
import com.example.spabooking.auth.security.CustomUserDetails;
import com.example.spabooking.auth.security.JwtUtils;
import com.example.spabooking.staff.dto.CreateStaffAccountRequest;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class StaffAccountManagementTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private JwtUtils jwtUtils;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Tenant tenant;
    private Staff staff;
    private User ownerUser;
    private User staffUser;
    private String ownerJwt;
    private String staffJwt;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        tenant = new Tenant();
        tenant.setName("TIKEY SPA Test");
        tenant.setSlug("tikey-spa-acct-" + System.currentTimeMillis());
        tenant = tenantRepository.saveAndFlush(tenant);

        staff = new Staff();
        staff.setTenant(tenant);
        staff.setName("Nhân viên Mới");
        staff.setIsActive(true);
        staff = staffRepository.saveAndFlush(staff);

        ownerUser = new User();
        ownerUser.setUsername("owner_acct_" + System.currentTimeMillis());
        ownerUser.setPassword("hash");
        ownerUser.setRole(UserRole.OWNER);
        ownerUser.setTenant(tenant);
        ownerUser.setIsActive(true);
        ownerUser = userRepository.saveAndFlush(ownerUser);

        staffUser = new User();
        staffUser.setUsername("staff_acct_" + System.currentTimeMillis());
        staffUser.setPassword("hash");
        staffUser.setRole(UserRole.STAFF);
        staffUser.setTenant(tenant);
        staffUser.setIsActive(true);
        staffUser = userRepository.saveAndFlush(staffUser);

        CustomUserDetails ownerDetails = new CustomUserDetails(ownerUser);
        ownerJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(ownerDetails, null, ownerDetails.getAuthorities()));

        CustomUserDetails staffDetails = new CustomUserDetails(staffUser);
        staffJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(staffDetails, null, staffDetails.getAuthorities()));
    }

    @Test
    void ownerCanCreateStaffAccount() throws Exception {
        CreateStaffAccountRequest request = new CreateStaffAccountRequest("staff_login_user", "password123");

        mockMvc.perform(post("/api/v1/staff/" + staff.getId() + "/account")
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is("staff_login_user")))
                .andExpect(jsonPath("$.role", is("STAFF")))
                .andExpect(jsonPath("$.staffId", is(staff.getId().intValue())));
    }

    @Test
    void staffCannotCreateStaffAccount() throws Exception {
        CreateStaffAccountRequest request = new CreateStaffAccountRequest("staff_login_user", "password123");

        mockMvc.perform(post("/api/v1/staff/" + staff.getId() + "/account")
                        .header("Authorization", "Bearer " + staffJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void cannotCreateDuplicateAccountForSameStaff() throws Exception {
        CreateStaffAccountRequest request1 = new CreateStaffAccountRequest("staff_login_1", "password123");
        mockMvc.perform(post("/api/v1/staff/" + staff.getId() + "/account")
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        CreateStaffAccountRequest request2 = new CreateStaffAccountRequest("staff_login_2", "password123");
        mockMvc.perform(post("/api/v1/staff/" + staff.getId() + "/account")
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletingStaffDeactivatesLinkedUserAccount() throws Exception {
        CreateStaffAccountRequest request = new CreateStaffAccountRequest("staff_to_delete", "password123");
        mockMvc.perform(post("/api/v1/staff/" + staff.getId() + "/account")
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/staff/" + staff.getId())
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isNoContent());

        User linked = userRepository.findByStaffId(staff.getId()).orElseThrow();
        assertFalse(linked.getIsActive());
    }
}
