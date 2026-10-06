package com.example.spabooking.staff;

import com.example.spabooking.auth.entity.User;
import com.example.spabooking.auth.enums.UserRole;
import com.example.spabooking.auth.repository.UserRepository;
import com.example.spabooking.auth.security.CustomUserDetails;
import com.example.spabooking.auth.security.JwtUtils;
import com.example.spabooking.booking.repository.BookingRepository;
import com.example.spabooking.customer.entity.Customer;
import com.example.spabooking.customer.repository.CustomerRepository;
import com.example.spabooking.service.entity.Service;
import com.example.spabooking.service.repository.ServiceRepository;
import com.example.spabooking.staff.dto.StaffWorkingHoursDto;
import com.example.spabooking.staff.entity.Staff;
import com.example.spabooking.staff.repository.StaffDayOffRepository;
import com.example.spabooking.staff.repository.StaffRepository;
import com.example.spabooking.staff.repository.StaffWorkingHoursRepository;
import com.example.spabooking.staff.service.StaffScheduleService;
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

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class StaffSchedulingSecurityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private StaffWorkingHoursRepository workingHoursRepository;

    @Autowired
    private StaffDayOffRepository dayOffRepository;

    @Autowired
    private StaffScheduleService staffScheduleService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private JwtUtils jwtUtils;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    private Tenant tenantA;
    private Tenant tenantB;

    private Staff staffA1;
    private Staff staffA2;
    private Staff staffB1;

    private User ownerA;
    private User staffUserA1;
    private User staffUserA2;
    private User ownerB;

    private String ownerAJwt;
    private String staffA1Jwt;
    private String staffA2Jwt;
    private String ownerBJwt;

    private Service serviceA;
    private Customer customerA;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        tenantA = new Tenant();
        tenantA.setName("Tenant A");
        tenantA.setSlug("tenant-a-" + System.currentTimeMillis());
        tenantA.setTimezone("Asia/Ho_Chi_Minh");
        tenantA.setIsActive(true);
        tenantA = tenantRepository.save(tenantA);

        tenantB = new Tenant();
        tenantB.setName("Tenant B");
        tenantB.setSlug("tenant-b-" + System.currentTimeMillis());
        tenantB.setTimezone("Asia/Ho_Chi_Minh");
        tenantB.setIsActive(true);
        tenantB = tenantRepository.save(tenantB);

        staffA1 = new Staff();
        staffA1.setName("Staff A1");
        staffA1.setTenant(tenantA);
        staffA1.setIsActive(true);
        staffA1.setIsDeleted(false);
        staffA1.setShowOnWebsite(true);
        staffA1 = staffRepository.save(staffA1);
        staffScheduleService.initDefaultWorkingHours(staffA1);

        staffA2 = new Staff();
        staffA2.setName("Staff A2");
        staffA2.setTenant(tenantA);
        staffA2.setIsActive(true);
        staffA2.setIsDeleted(false);
        staffA2.setShowOnWebsite(true);
        staffA2 = staffRepository.save(staffA2);
        staffScheduleService.initDefaultWorkingHours(staffA2);

        staffB1 = new Staff();
        staffB1.setName("Staff B1");
        staffB1.setTenant(tenantB);
        staffB1.setIsActive(true);
        staffB1.setIsDeleted(false);
        staffB1.setShowOnWebsite(true);
        staffB1 = staffRepository.save(staffB1);
        staffScheduleService.initDefaultWorkingHours(staffB1);

        ownerA = new User();
        ownerA.setUsername("ownerA_" + System.currentTimeMillis());
        ownerA.setPassword("secret");
        ownerA.setRole(UserRole.OWNER);
        ownerA.setTenant(tenantA);
        ownerA.setIsActive(true);
        ownerA = userRepository.save(ownerA);

        staffUserA1 = new User();
        staffUserA1.setUsername("staffA1_" + System.currentTimeMillis());
        staffUserA1.setPassword("secret");
        staffUserA1.setRole(UserRole.STAFF);
        staffUserA1.setTenant(tenantA);
        staffUserA1.setStaff(staffA1);
        staffUserA1.setIsActive(true);
        staffUserA1 = userRepository.save(staffUserA1);

        staffUserA2 = new User();
        staffUserA2.setUsername("staffA2_" + System.currentTimeMillis());
        staffUserA2.setPassword("secret");
        staffUserA2.setRole(UserRole.STAFF);
        staffUserA2.setTenant(tenantA);
        staffUserA2.setStaff(staffA2);
        staffUserA2.setIsActive(true);
        staffUserA2 = userRepository.save(staffUserA2);

        ownerB = new User();
        ownerB.setUsername("ownerB_" + System.currentTimeMillis());
        ownerB.setPassword("secret");
        ownerB.setRole(UserRole.OWNER);
        ownerB.setTenant(tenantB);
        ownerB.setIsActive(true);
        ownerB = userRepository.save(ownerB);

        ownerAJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(new CustomUserDetails(ownerA), null, new CustomUserDetails(ownerA).getAuthorities()));
        staffA1Jwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(new CustomUserDetails(staffUserA1), null, new CustomUserDetails(staffUserA1).getAuthorities()));
        staffA2Jwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(new CustomUserDetails(staffUserA2), null, new CustomUserDetails(staffUserA2).getAuthorities()));
        ownerBJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(new CustomUserDetails(ownerB), null, new CustomUserDetails(ownerB).getAuthorities()));

        serviceA = new Service();
        serviceA.setName("Facial Care");
        serviceA.setDurationMinutes(60);
        serviceA.setPrice(BigDecimal.valueOf(500000));
        serviceA.setTenant(tenantA);
        serviceA.setIsActive(true);
        serviceA = serviceRepository.save(serviceA);

        customerA = new Customer();
        customerA.setName("Customer Alpha");
        customerA.setPhone("0900000001");
        customerA.setTenant(tenantA);
        customerA.setIsActive(true);
        customerA = customerRepository.save(customerA);
    }

    private String createBookingJson(Long customerId, Long serviceId, Long staffId, LocalDateTime startTime) {
        LocalDateTime endTime = startTime.plusMinutes(60);
        return String.format("{\"customerId\":%d,\"serviceId\":%d,\"staffId\":%d,\"startTime\":\"%s\",\"endTime\":\"%s\"}",
                customerId, serviceId, staffId, startTime.toString(), endTime.toString());
    }

    private String createDayOffJson(LocalDate date, String reason) {
        return String.format("{\"date\":\"%s\",\"reason\":\"%s\"}", date.toString(), reason);
    }

    private String createRescheduleJson(LocalDateTime startTime) {
        return String.format("{\"startTime\":\"%s\"}", startTime.toString());
    }

    private String createWorkingHoursJson(List<StaffWorkingHoursDto> dtos) {
        StringBuilder sb = new StringBuilder("{\"workingHours\":[");
        for (int i = 0; i < dtos.size(); i++) {
            StaffWorkingHoursDto d = dtos.get(i);
            if (i > 0) sb.append(",");
            sb.append(String.format("{\"dayOfWeek\":\"%s\",\"startTime\":\"%s\",\"endTime\":\"%s\",\"isActive\":%b}",
                    d.getDayOfWeek().name(), d.getStartTime().toString(), d.getEndTime().toString(), d.getIsActive()));
        }
        sb.append("]}");
        return sb.toString();
    }

    @Test
    void testOwnerCanGetAndUpdateWorkingHours() throws Exception {
        // GET working hours (auto initializes 7 days)
        mockMvc.perform(get("/api/v1/staff/{staffId}/working-hours", staffA1.getId())
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7)))
                .andExpect(jsonPath("$[0].dayOfWeek", is("MONDAY")))
                .andExpect(jsonPath("$[0].startTime", is("09:00:00")))
                .andExpect(jsonPath("$[0].endTime", is("18:00:00")));

        // Update working hours: Monday to Friday 08:30 - 17:30, Saturday 09:00 - 15:00, Sunday off
        List<StaffWorkingHoursDto> updatedList = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            boolean active = day != DayOfWeek.SUNDAY;
            LocalTime start = day == DayOfWeek.SATURDAY ? LocalTime.of(9, 0) : LocalTime.of(8, 30);
            LocalTime end = day == DayOfWeek.SATURDAY ? LocalTime.of(15, 0) : LocalTime.of(17, 30);
            updatedList.add(new StaffWorkingHoursDto(null, day, start, end, active));
        }

        mockMvc.perform(put("/api/v1/staff/{staffId}/working-hours", staffA1.getId())
                        .header("Authorization", "Bearer " + ownerAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createWorkingHoursJson(updatedList)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7)))
                .andExpect(jsonPath("$[0].dayOfWeek", is("MONDAY")))
                .andExpect(jsonPath("$[0].startTime", is("08:30:00")))
                .andExpect(jsonPath("$[0].endTime", is("17:30:00")));

        // Invalid time range (start after end) must be rejected
        updatedList.set(0, new StaffWorkingHoursDto(null, DayOfWeek.MONDAY, LocalTime.of(18, 0), LocalTime.of(9, 0), true));
        mockMvc.perform(put("/api/v1/staff/{staffId}/working-hours", staffA1.getId())
                        .header("Authorization", "Bearer " + ownerAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createWorkingHoursJson(updatedList)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testStaffCannotUpdateWorkingHoursAndCannotAccessOtherStaff() throws Exception {
        // Staff A1 can view their own working hours
        mockMvc.perform(get("/api/v1/staff/{staffId}/working-hours", staffA1.getId())
                        .header("Authorization", "Bearer " + staffA1Jwt))
                .andExpect(status().isOk());

        // Staff A1 CANNOT view Staff A2's working hours (403)
        mockMvc.perform(get("/api/v1/staff/{staffId}/working-hours", staffA2.getId())
                        .header("Authorization", "Bearer " + staffA1Jwt))
                .andExpect(status().isForbidden());

        // Staff A1 CANNOT update working hours (403)
        List<StaffWorkingHoursDto> list = List.of(
                new StaffWorkingHoursDto(null, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(17, 0), true)
        );
        mockMvc.perform(put("/api/v1/staff/{staffId}/working-hours", staffA1.getId())
                        .header("Authorization", "Bearer " + staffA1Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createWorkingHoursJson(list)))
                .andExpect(status().isForbidden());

        // Owner B cannot view or update Staff A1 working hours (404 Tenant Isolation)
        mockMvc.perform(get("/api/v1/staff/{staffId}/working-hours", staffA1.getId())
                        .header("Authorization", "Bearer " + ownerBJwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void testOwnerCanManageDaysOffAndDuplicateRejected() throws Exception {
        LocalDate dayOffDate = LocalDate.now().plusDays(5);

        // OWNER adds day off
        String responseContent = mockMvc.perform(post("/api/v1/staff/{staffId}/days-off", staffA1.getId())
                        .header("Authorization", "Bearer " + ownerAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createDayOffJson(dayOffDate, "Vacation leave")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.date", is(dayOffDate.toString())))
                .andExpect(jsonPath("$.reason", is("Vacation leave")))
                .andReturn().getResponse().getContentAsString();

        Long dayOffId = objectMapper.readTree(responseContent).get("id").asLong();

        // Duplicate day off on same date rejected
        mockMvc.perform(post("/api/v1/staff/{staffId}/days-off", staffA1.getId())
                        .header("Authorization", "Bearer " + ownerAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createDayOffJson(dayOffDate, "Vacation leave")))
                .andExpect(status().isBadRequest());

        // STAFF cannot add day off
        mockMvc.perform(post("/api/v1/staff/{staffId}/days-off", staffA1.getId())
                        .header("Authorization", "Bearer " + staffA1Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createDayOffJson(LocalDate.now().plusDays(10), "Test")))
                .andExpect(status().isForbidden());

        // STAFF can view own days off
        mockMvc.perform(get("/api/v1/staff/{staffId}/days-off", staffA1.getId())
                        .header("Authorization", "Bearer " + staffA1Jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // OWNER deletes day off
        mockMvc.perform(delete("/api/v1/staff/{staffId}/days-off/{dayOffId}", staffA1.getId(), dayOffId)
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isNoContent());

        // Verification: days off list is now empty
        mockMvc.perform(get("/api/v1/staff/{staffId}/days-off", staffA1.getId())
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testDayOffBlocksBookingCreationAndReschedule() throws Exception {
        LocalDate dayOffDate = LocalDate.now().plusDays(3);
        // Find next day that is Monday-Friday to ensure working hours are active
        while (dayOffDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            dayOffDate = dayOffDate.plusDays(1);
        }

        // Add day off
        mockMvc.perform(post("/api/v1/staff/{staffId}/days-off", staffA1.getId())
                        .header("Authorization", "Bearer " + ownerAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createDayOffJson(dayOffDate, "Sick leave")))
                .andExpect(status().isCreated());

        // Attempt to create booking on that day off -> REJECTED (409)
        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + ownerAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBookingJson(customerA.getId(), serviceA.getId(), staffA1.getId(), dayOffDate.atTime(10, 0))))
                .andExpect(status().isConflict());

        // Create a booking on another valid working day
        LocalDate validDate = dayOffDate.plusDays(1);
        while (validDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            validDate = validDate.plusDays(1);
        }

        String createdJson = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + ownerAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBookingJson(customerA.getId(), serviceA.getId(), staffA1.getId(), validDate.atTime(10, 0))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long bookingId = objectMapper.readTree(createdJson).get("id").asLong();

        // Reschedule to day off date -> REJECTED (409)
        mockMvc.perform(post("/api/v1/bookings/{id}/reschedule", bookingId)
                        .header("Authorization", "Bearer " + ownerAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRescheduleJson(dayOffDate.atTime(10, 0))))
                .andExpect(status().isConflict());
    }

    @Test
    void testOutsideWorkingHoursRejected() throws Exception {
        // Find next Monday
        LocalDate targetDate = LocalDate.now().plusDays(1);
        while (targetDate.getDayOfWeek() != DayOfWeek.MONDAY) {
            targetDate = targetDate.plusDays(1);
        }

        // Staff default hours: 09:00 - 18:00
        // 1. Start before opening time (08:30) -> REJECTED (409)
        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + ownerAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBookingJson(customerA.getId(), serviceA.getId(), staffA1.getId(), targetDate.atTime(8, 30))))
                .andExpect(status().isConflict());

        // 2. End after closing time (17:30 start + 60 min service duration = 18:30 end, beyond 18:00) -> REJECTED (409)
        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + ownerAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBookingJson(customerA.getId(), serviceA.getId(), staffA1.getId(), targetDate.atTime(17, 30))))
                .andExpect(status().isConflict());

        // 3. Exact boundary: 17:00 start + 60 min = 18:00 end -> ALLOWED (201)
        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + ownerAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBookingJson(customerA.getId(), serviceA.getId(), staffA1.getId(), targetDate.atTime(17, 0))))
                .andExpect(status().isCreated());

        // 4. Sunday (inactive day) -> REJECTED (409)
        workingHoursRepository.findByStaffIdAndTenantIdAndDayOfWeek(staffA1.getId(), tenantA.getId(), DayOfWeek.SUNDAY)
                .ifPresent(wh -> {
                    wh.setIsActive(false);
                    workingHoursRepository.save(wh);
                });

        LocalDate nextSunday = targetDate.plusDays(6);
        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + ownerAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBookingJson(customerA.getId(), serviceA.getId(), staffA1.getId(), nextSunday.atTime(10, 0))))
                .andExpect(status().isConflict());
    }

    @Test
    void testAdjacentBookingsAllowedAndOverlappingRejected() throws Exception {
        LocalDate tuesday = LocalDate.now().plusDays(1);
        while (tuesday.getDayOfWeek() != DayOfWeek.TUESDAY) {
            tuesday = tuesday.plusDays(1);
        }

        // Booking 1: 09:00 - 10:00 -> SUCCESS
        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + ownerAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBookingJson(customerA.getId(), serviceA.getId(), staffA1.getId(), tuesday.atTime(9, 0))))
                .andExpect(status().isCreated());

        // Customer B
        Customer customerB = new Customer();
        customerB.setName("Customer Beta");
        customerB.setPhone("0900000002");
        customerB.setTenant(tenantA);
        customerB.setIsActive(true);
        customerB = customerRepository.save(customerB);

        // Booking 2 (Overlapping): 09:30 - 10:30 for staff A1 -> REJECTED (409 Conflict)
        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + ownerAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBookingJson(customerB.getId(), serviceA.getId(), staffA1.getId(), tuesday.atTime(9, 30))))
                .andExpect(status().isConflict());

        // Booking 3 (Adjacent half-open interval): 10:00 - 11:00 for staff A1 -> ALLOWED (201 Created)
        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + ownerAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBookingJson(customerB.getId(), serviceA.getId(), staffA1.getId(), tuesday.atTime(10, 0))))
                .andExpect(status().isCreated());
    }

    @Test
    void testNonBlockingStatusesDoNotBlockSlot() throws Exception {
        LocalDate wednesday = LocalDate.now().plusDays(1);
        while (wednesday.getDayOfWeek() != DayOfWeek.WEDNESDAY) {
            wednesday = wednesday.plusDays(1);
        }

        // Create booking 1: 10:00 - 11:00
        String bJson = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + ownerAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBookingJson(customerA.getId(), serviceA.getId(), staffA1.getId(), wednesday.atTime(10, 0))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long bookingId = objectMapper.readTree(bJson).get("id").asLong();

        // Cancel booking 1
        mockMvc.perform(post("/api/v1/bookings/{id}/cancel", bookingId)
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));

        // New booking 2 on the EXACT same slot (10:00 - 11:00) -> ALLOWED because CANCELLED is non-blocking!
        Customer customerB = new Customer();
        customerB.setName("Customer Gamma");
        customerB.setPhone("0900000003");
        customerB.setTenant(tenantA);
        customerB.setIsActive(true);
        customerB = customerRepository.save(customerB);

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + ownerAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBookingJson(customerB.getId(), serviceA.getId(), staffA1.getId(), wednesday.atTime(10, 0))))
                .andExpect(status().isCreated());
    }

    @Test
    void testDailyScheduleOwnerAndStaffIsolation() throws Exception {
        LocalDate thursday = LocalDate.now().plusDays(1);
        while (thursday.getDayOfWeek() != DayOfWeek.THURSDAY) {
            thursday = thursday.plusDays(1);
        }

        // Booking for Staff A1: 09:00 - 10:00
        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + ownerAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBookingJson(customerA.getId(), serviceA.getId(), staffA1.getId(), thursday.atTime(9, 0))))
                .andExpect(status().isCreated());

        // Booking for Staff A2: 14:00 - 15:00
        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + ownerAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBookingJson(customerA.getId(), serviceA.getId(), staffA2.getId(), thursday.atTime(14, 0))))
                .andExpect(status().isCreated());

        // OWNER requests daily schedule without filter -> sees both bookings (2 items)
        mockMvc.perform(get("/api/v1/schedule/daily")
                        .param("date", thursday.toString())
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // OWNER requests daily schedule filtered by staff A1 -> sees 1 item
        mockMvc.perform(get("/api/v1/schedule/daily")
                        .param("date", thursday.toString())
                        .param("staffId", staffA1.getId().toString())
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].staffName", is("Staff A1")));

        // STAFF A1 requests daily schedule -> forced to see only Staff A1's booking (1 item)
        mockMvc.perform(get("/api/v1/schedule/daily")
                        .param("date", thursday.toString())
                        .header("Authorization", "Bearer " + staffA1Jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].staffName", is("Staff A1")));

        // STAFF A1 attempts to pass staffId = staffA2.getId() -> STILL forced to see only their own!
        mockMvc.perform(get("/api/v1/schedule/daily")
                        .param("date", thursday.toString())
                        .param("staffId", staffA2.getId().toString())
                        .header("Authorization", "Bearer " + staffA1Jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].staffName", is("Staff A1")));
    }
}
