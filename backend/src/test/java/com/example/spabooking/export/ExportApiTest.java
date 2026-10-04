package com.example.spabooking.export;

import com.example.spabooking.auth.entity.User;
import com.example.spabooking.auth.enums.UserRole;
import com.example.spabooking.auth.repository.UserRepository;
import com.example.spabooking.auth.security.CustomUserDetails;
import com.example.spabooking.auth.security.JwtUtils;
import com.example.spabooking.booking.entity.Booking;
import com.example.spabooking.booking.enums.BookingStatus;
import com.example.spabooking.booking.repository.BookingRepository;
import com.example.spabooking.customer.entity.Customer;
import com.example.spabooking.customer.repository.CustomerRepository;
import com.example.spabooking.service.repository.ServiceRepository;
import com.example.spabooking.staff.entity.Staff;
import com.example.spabooking.staff.repository.StaffRepository;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class ExportApiTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private JwtUtils jwtUtils;

    private MockMvc mockMvc;

    private String ownerJwt;
    private String staffJwt;

    private Tenant tenantA;
    private Tenant tenantB;

    private Booking completedBookingA;
    private Booking cancelledBookingA;
    private Booking completedBookingB;
    private Customer customerA;
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        long unique = System.currentTimeMillis();

        tenantA = new Tenant();
        tenantA.setName("Tenant A");
        tenantA.setSlug("tenant-a-export-" + unique);
        tenantA = tenantRepository.saveAndFlush(tenantA);

        tenantB = new Tenant();
        tenantB.setName("Tenant B");
        tenantB.setSlug("tenant-b-export-" + unique);
        tenantB = tenantRepository.saveAndFlush(tenantB);

        User ownerA = new User();
        ownerA.setUsername("ownerAExport" + unique);
        ownerA.setPassword("encoded");
        ownerA.setRole(UserRole.OWNER);
        ownerA.setTenant(tenantA);
        ownerA = userRepository.saveAndFlush(ownerA);

        User staffUserA = new User();
        staffUserA.setUsername("staffAExport" + unique);
        staffUserA.setPassword("encoded");
        staffUserA.setRole(UserRole.STAFF);
        staffUserA.setTenant(tenantA);
        staffUserA = userRepository.saveAndFlush(staffUserA);

        ownerJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(ownerA), null, new CustomUserDetails(ownerA).getAuthorities()));
        staffJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(staffUserA), null, new CustomUserDetails(staffUserA).getAuthorities()));

        customerA = new Customer();
        customerA.setTenant(tenantA);
        customerA.setName("Alice");
        customerA.setPhone("0909000111");
        customerA.setEmail("alice@example.com");
        customerA.setIsActive(true);
        customerA = customerRepository.saveAndFlush(customerA);

        Customer customerB = new Customer();
        customerB.setTenant(tenantB);
        customerB.setName("Bob");
        customerB.setPhone("0909000222");
        customerB.setIsActive(true);
        customerB = customerRepository.saveAndFlush(customerB);

        Staff staffA = new Staff();
        staffA.setTenant(tenantA);
        staffA.setName("Staff A");
        staffA.setIsActive(true);
        staffA = staffRepository.saveAndFlush(staffA);

        com.example.spabooking.service.entity.Service serviceA = new com.example.spabooking.service.entity.Service();
        serviceA.setTenant(tenantA);
        serviceA.setName("Massage");
        serviceA.setPrice(new BigDecimal("300000.00"));
        serviceA.setDurationMinutes(60);
        serviceA.setIsActive(true);
        serviceA = serviceRepository.saveAndFlush(serviceA);

        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);

        completedBookingA = saveBooking(tenantA, customerA, staffA, serviceA,
                now.toLocalDate().atStartOfDay().plusHours(14), BookingStatus.COMPLETED);

        cancelledBookingA = saveBooking(tenantA, customerA, staffA, serviceA,
                now.toLocalDate().atStartOfDay().plusHours(16), BookingStatus.CANCELLED);

        Staff staffB = new Staff();
        staffB.setTenant(tenantB);
        staffB.setName("Staff B");
        staffB.setIsActive(true);
        staffB = staffRepository.saveAndFlush(staffB);

        com.example.spabooking.service.entity.Service serviceB = new com.example.spabooking.service.entity.Service();
        serviceB.setTenant(tenantB);
        serviceB.setName("Tenant B Service");
        serviceB.setPrice(new BigDecimal("999999.00"));
        serviceB.setDurationMinutes(30);
        serviceB.setIsActive(true);
        serviceB = serviceRepository.saveAndFlush(serviceB);

        completedBookingB = saveBooking(tenantB, customerB, staffB, serviceB,
                now.toLocalDate().atStartOfDay().plusHours(10), BookingStatus.COMPLETED);
    }

    private Booking saveBooking(Tenant tenant, Customer customer, Staff staff,
                                com.example.spabooking.service.entity.Service service,
                                LocalDateTime startTime, BookingStatus status) {
        Booking booking = new Booking();
        booking.setTenant(tenant);
        booking.setCustomer(customer);
        booking.setStaff(staff);
        booking.setService(service);
        booking.setStartTime(startTime);
        booking.setEndTime(startTime.plusHours(1));
        booking.setStatus(status);
        booking.setPrice(service.getPrice());
        return bookingRepository.saveAndFlush(booking);
    }

    private byte[] xlsxBytes(MvcResult result) throws Exception {
        byte[] body = result.getResponse().getContentAsByteArray();
        assertThat(body.length, greaterThan(0));
        assertThat(body[0], is((byte) 0x50));
        assertThat(body[1], is((byte) 0x4B));
        return body;
    }

    @Test
    void ownerCanExportBookings() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/exports/bookings")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString("bookings-")))
                .andReturn();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes(result)))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            assertThat(header.getCell(0).getStringCellValue(), is("Mã lịch hẹn"));
            assertThat(header.getCell(1).getStringCellValue(), is("Khách hàng"));
            assertThat(header.getCell(9).getStringCellValue(), is("Trạng thái"));
            assertThat(sheet.getPhysicalNumberOfRows(), is(3));

            Row completedRow = findRowByCellValue(sheet, 0, completedBookingA.getBookingCode());
            assertThat(completedRow, notNullValue());
            assertThat(completedRow.getCell(1).getStringCellValue(), is("Alice"));
            assertThat(completedRow.getCell(2).getStringCellValue(), is("0909000111"));
            assertThat(completedRow.getCell(9).getStringCellValue(), is("Hoàn thành"));

            Row cancelledRow = findRowByCellValue(sheet, 0, cancelledBookingA.getBookingCode());
            assertThat(cancelledRow, notNullValue());
            assertThat(cancelledRow.getCell(9).getStringCellValue(), is("Đã hủy"));
        }
    }

    @Test
    void ownerCanExportCustomers() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/exports/customers")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("customers-")))
                .andReturn();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes(result)))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue(), is("Họ tên"));
            assertThat(sheet.getPhysicalNumberOfRows(), is(2));

            Row row = sheet.getRow(1);
            assertThat(row.getCell(0).getStringCellValue(), is("Alice"));
            assertThat(row.getCell(1).getStringCellValue(), is("0909000111"));
            assertThat(row.getCell(2).getStringCellValue(), is("alice@example.com"));
        }
    }

    @Test
    void ownerCanExportRevenue() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/exports/revenue")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("revenue-")))
                .andReturn();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes(result)))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue(), is("Ngày"));
            // One completed booking today, plus the "Tổng cộng" total row
            assertThat(sheet.getPhysicalNumberOfRows(), is(3));

            Row totalRow = sheet.getRow(sheet.getLastRowNum());
            assertThat(totalRow.getCell(0).getStringCellValue(), is("Tổng cộng"));
            assertThat(totalRow.getCell(1).getNumericCellValue(), is(1.0));
            assertThat(totalRow.getCell(2).getNumericCellValue(), is(300000.0));
        }
    }

    @Test
    void staffCannotExportAnything() throws Exception {
        mockMvc.perform(get("/api/v1/exports/bookings")
                        .header("Authorization", "Bearer " + staffJwt))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/exports/customers")
                        .header("Authorization", "Bearer " + staffJwt))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/exports/revenue")
                        .header("Authorization", "Bearer " + staffJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedCannotExportAnything() throws Exception {
        mockMvc.perform(get("/api/v1/exports/bookings"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/exports/customers"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/exports/revenue"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bookingExportRespectsTenantIsolation() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/exports/bookings")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andReturn();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes(result)))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row tenantBRow = findRowByCellValue(sheet, 0, completedBookingB.getBookingCode());
            assertThat(tenantBRow, nullValue());
        }
    }

    @Test
    void emptyTenantProducesValidHeaderOnlyReport() throws Exception {
        long unique = System.currentTimeMillis();
        Tenant tenantC = new Tenant();
        tenantC.setName("Tenant C");
        tenantC.setSlug("tenant-c-export-" + unique);
        tenantC = tenantRepository.saveAndFlush(tenantC);

        User ownerC = new User();
        ownerC.setUsername("ownerCExport" + unique);
        ownerC.setPassword("encoded");
        ownerC.setRole(UserRole.OWNER);
        ownerC.setTenant(tenantC);
        ownerC = userRepository.saveAndFlush(ownerC);

        String ownerCJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(ownerC), null, new CustomUserDetails(ownerC).getAuthorities()));

        MvcResult bookingsResult = mockMvc.perform(get("/api/v1/exports/bookings")
                        .header("Authorization", "Bearer " + ownerCJwt))
                .andExpect(status().isOk())
                .andReturn();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes(bookingsResult)))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getPhysicalNumberOfRows(), is(1));
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue(), is("Mã lịch hẹn"));
        }

        MvcResult customersResult = mockMvc.perform(get("/api/v1/exports/customers")
                        .header("Authorization", "Bearer " + ownerCJwt))
                .andExpect(status().isOk())
                .andReturn();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes(customersResult)))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getPhysicalNumberOfRows(), is(1));
        }

        MvcResult revenueResult = mockMvc.perform(get("/api/v1/exports/revenue")
                        .header("Authorization", "Bearer " + ownerCJwt))
                .andExpect(status().isOk())
                .andReturn();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes(revenueResult)))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getPhysicalNumberOfRows(), is(2));
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue(), is("Tổng cộng"));
            assertThat(sheet.getRow(1).getCell(2).getNumericCellValue(), is(0.0));
        }
    }

    @Test
    void revenueExportCoversFullThirtyDayWindow() throws Exception {
        // This test proves that the 30-day revenue export includes COMPLETED bookings
        // from across the window, not only today.
        long unique2 = System.currentTimeMillis() + 1;
        Tenant tenantD = new Tenant();
        tenantD.setName("Tenant D");
        tenantD.setSlug("tenant-d-rev30-" + unique2);
        tenantD = tenantRepository.saveAndFlush(tenantD);

        User ownerD = new User();
        ownerD.setUsername("ownerDRev30-" + unique2);
        ownerD.setPassword("encoded");
        ownerD.setRole(UserRole.OWNER);
        ownerD.setTenant(tenantD);
        ownerD = userRepository.saveAndFlush(ownerD);

        String ownerDJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(ownerD), null, new CustomUserDetails(ownerD).getAuthorities()));

        Customer customerD = new Customer();
        customerD.setTenant(tenantD);
        customerD.setName("Dan");
        customerD.setPhone("0900000099");
        customerD.setIsActive(true);
        customerD = customerRepository.saveAndFlush(customerD);

        Staff staffD = new Staff();
        staffD.setTenant(tenantD);
        staffD.setName("Dan Staff");
        staffD.setIsActive(true);
        staffD = staffRepository.saveAndFlush(staffD);

        com.example.spabooking.service.entity.Service svcD = new com.example.spabooking.service.entity.Service();
        svcD.setTenant(tenantD);
        svcD.setName("Svc D");
        svcD.setPrice(new BigDecimal("100000.00"));
        svcD.setDurationMinutes(30);
        svcD.setIsActive(true);
        svcD = serviceRepository.saveAndFlush(svcD);

        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);

        // Booking at day 0 (today)
        saveBooking(tenantD, customerD, staffD, svcD, now.toLocalDate().atStartOfDay().plusHours(10), BookingStatus.COMPLETED);
        // Booking at day -10 (within 30-day window)
        saveBooking(tenantD, customerD, staffD, svcD, now.minusDays(10).toLocalDate().atStartOfDay().plusHours(10), BookingStatus.COMPLETED);
        // Booking at day -25 (within 30-day window)
        saveBooking(tenantD, customerD, staffD, svcD, now.minusDays(25).toLocalDate().atStartOfDay().plusHours(10), BookingStatus.COMPLETED);
        // Booking at day -31 (OUTSIDE window — must be excluded)
        saveBooking(tenantD, customerD, staffD, svcD, now.minusDays(31).toLocalDate().atStartOfDay().plusHours(10), BookingStatus.COMPLETED);
        // CANCELLED booking today — must be excluded regardless
        saveBooking(tenantD, customerD, staffD, svcD, now.toLocalDate().atStartOfDay().plusHours(14), BookingStatus.CANCELLED);

        MvcResult result = mockMvc.perform(get("/api/v1/exports/revenue")
                        .header("Authorization", "Bearer " + ownerDJwt))
                .andExpect(status().isOk())
                .andReturn();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes(result)))) {
            Sheet sheet = workbook.getSheetAt(0);

            // Header row (row 0) + 3 data rows (days 0, -10, -25) + 1 total row = 5 rows
            assertThat("Expected 3 data rows + header + total",
                    sheet.getPhysicalNumberOfRows(), is(5));

            Row totalRow = sheet.getRow(sheet.getLastRowNum());
            assertThat(totalRow.getCell(0).getStringCellValue(), is("Tổng cộng"));
            assertThat("Total count should be 3 completed bookings in 30-day window",
                    totalRow.getCell(1).getNumericCellValue(), is(3.0));
            assertThat("Grand total must equal sum of 3 × 100000",
                    totalRow.getCell(2).getNumericCellValue(), is(300000.0));
        }
    }

    private Row findRowByCellValue(Sheet sheet, int column, String value) {
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null && row.getCell(column) != null
                    && value.equals(row.getCell(column).getStringCellValue())) {
                return row;
            }
        }
        return null;
    }
}
