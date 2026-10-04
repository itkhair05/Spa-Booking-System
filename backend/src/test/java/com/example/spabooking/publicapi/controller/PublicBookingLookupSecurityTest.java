package com.example.spabooking.publicapi.controller;

import com.example.spabooking.customer.entity.Customer;
import com.example.spabooking.customer.repository.CustomerRepository;
import com.example.spabooking.publicapi.dto.CreatePublicBookingRequest;
import com.example.spabooking.service.entity.Service;
import com.example.spabooking.service.repository.ServiceRepository;
import com.example.spabooking.staff.entity.Staff;
import com.example.spabooking.staff.repository.StaffRepository;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class PublicBookingLookupSecurityTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    private Tenant tenantA;
    private Tenant tenantB;
    private Service serviceA;
    private Staff staffA;
    private Service serviceB;
    private Staff staffB;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        tenantA = new Tenant();
        tenantA.setName("TIKEY SPA");
        tenantA.setSlug("tikey-public-" + System.currentTimeMillis());
        tenantA.setPhone("0908888777");
        tenantA.setAddress("789 Bến Vân Đồn, Q4");
        tenantA = tenantRepository.saveAndFlush(tenantA);

        tenantB = new Tenant();
        tenantB.setName("Other Spa");
        tenantB.setSlug("other-public-" + System.currentTimeMillis());
        tenantB = tenantRepository.saveAndFlush(tenantB);

        serviceA = new Service();
        serviceA.setTenant(tenantA);
        serviceA.setName("Massage Trị Liệu");
        serviceA.setDurationMinutes(60);
        serviceA.setPrice(new BigDecimal("350000.00"));
        serviceA.setIsActive(true);
        serviceA = serviceRepository.saveAndFlush(serviceA);

        staffA = new Staff();
        staffA.setTenant(tenantA);
        staffA.setName("KTV Hằng");
        staffA.setIsActive(true);
        staffA = staffRepository.saveAndFlush(staffA);

        serviceB = new Service();
        serviceB.setTenant(tenantB);
        serviceB.setName("Tẩy Tế Bào Chết");
        serviceB.setDurationMinutes(45);
        serviceB.setPrice(new BigDecimal("250000.00"));
        serviceB.setIsActive(true);
        serviceB = serviceRepository.saveAndFlush(serviceB);

        staffB = new Staff();
        staffB.setTenant(tenantB);
        staffB.setName("KTV Tuấn");
        staffB.setIsActive(true);
        staffB = staffRepository.saveAndFlush(staffB);
    }

    @Test
    void publicBookingCreatesUnpredictableBookingCodeAndSupportsSafeLookup() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);

        String json = "{"
                + "\"customerName\": \"Nguyễn Thị Hoa\","
                + "\"customerPhone\": \"0918765432\","
                + "\"customerEmail\": \"hoa@gmail.com\","
                + "\"serviceId\": " + serviceA.getId() + ","
                + "\"staffId\": " + staffA.getId() + ","
                + "\"startTime\": \"" + start.toString() + "\""
                + "}";

        MvcResult result = mockMvc.perform(post("/api/v1/public/spas/" + tenantA.getSlug() + "/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingCode", startsWith("BK-")))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andReturn();

        JsonNode created = objectMapper.readTree(result.getResponse().getContentAsString());
        String bookingCode = created.get("bookingCode").asText();
        assertNotNull(bookingCode);

        // Safe Public Lookup using unpredictable booking code
        MvcResult lookupResult = mockMvc.perform(get("/api/v1/public/spas/" + tenantA.getSlug() + "/bookings/" + bookingCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingCode", is(bookingCode)))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.serviceName", is("Massage Trị Liệu")))
                .andExpect(jsonPath("$.durationMinutes", is(60)))
                .andExpect(jsonPath("$.staffName", is("KTV Hằng")))
                .andExpect(jsonPath("$.spaName", is("TIKEY SPA")))
                .andExpect(jsonPath("$.spaPhone", is("0908888777")))
                .andExpect(jsonPath("$.spaAddress", is("789 Bến Vân Đồn, Q4")))
                .andReturn();

        JsonNode detail = objectMapper.readTree(lookupResult.getResponse().getContentAsString());

        // Explicit boundary checks: NEVER expose internal IDs or sensitive data
        assertFalse(detail.has("id"), "Public response must not expose internal booking ID");
        assertFalse(detail.has("customerId"), "Public response must not expose internal customer ID");
        assertFalse(detail.has("tenantId"), "Public response must not expose internal tenant ID");
        assertFalse(detail.has("staffId"), "Public response must not expose internal staff ID");
        assertFalse(detail.has("serviceId"), "Public response must not expose internal service ID");
        assertFalse(detail.has("password"), "Public response must not expose password");
    }

    @Test
    void lookupWithNonexistentBookingCodeReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/public/spas/" + tenantA.getSlug() + "/bookings/BK-NONEXISTENT"))
                .andExpect(status().isNotFound());
    }

    @Test
    void lookupCrossTenantBookingCodeReturns404() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(11).withMinute(0).withSecond(0).withNano(0);

        String jsonB = "{"
                + "\"customerName\": \"Khách Tenant B\","
                + "\"customerPhone\": \"0933444555\","
                + "\"serviceId\": " + serviceB.getId() + ","
                + "\"staffId\": " + staffB.getId() + ","
                + "\"startTime\": \"" + start.toString() + "\""
                + "}";

        MvcResult resultB = mockMvc.perform(post("/api/v1/public/spas/" + tenantB.getSlug() + "/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonB))
                .andExpect(status().isCreated())
                .andReturn();

        String codeB = objectMapper.readTree(resultB.getResponse().getContentAsString()).get("bookingCode").asText();

        // Attempt cross-tenant lookup: query codeB under tenantA's slug
        mockMvc.perform(get("/api/v1/public/spas/" + tenantA.getSlug() + "/bookings/" + codeB))
                .andExpect(status().isNotFound());
    }

    @Test
    void enumerationAttackUsingSequentialIdsFails() throws Exception {
        // Attacker attempts sequential integer enumeration
        mockMvc.perform(get("/api/v1/public/spas/" + tenantA.getSlug() + "/bookings/1"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/public/spas/" + tenantA.getSlug() + "/bookings/2"))
                .andExpect(status().isNotFound());
    }
}
