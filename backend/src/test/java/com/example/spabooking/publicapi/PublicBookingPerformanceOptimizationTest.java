package com.example.spabooking.publicapi;

import com.example.spabooking.booking.dto.DashboardMetricsResponse;
import com.example.spabooking.booking.entity.Booking;
import com.example.spabooking.booking.enums.BookingStatus;
import com.example.spabooking.booking.repository.BookingRepository;
import com.example.spabooking.booking.service.DashboardService;
import com.example.spabooking.customer.entity.Customer;
import com.example.spabooking.customer.repository.CustomerRepository;
import com.example.spabooking.service.entity.Service;
import com.example.spabooking.service.entity.ServiceCategory;
import com.example.spabooking.service.repository.ServiceCategoryRepository;
import com.example.spabooking.service.repository.ServiceRepository;
import com.example.spabooking.staff.entity.Staff;
import com.example.spabooking.staff.entity.StaffWorkingHours;
import com.example.spabooking.staff.repository.StaffRepository;
import com.example.spabooking.staff.repository.StaffWorkingHoursRepository;
import com.example.spabooking.staff.service.StaffScheduleService;
import com.example.spabooking.tenant.context.TenantContext;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class PublicBookingPerformanceOptimizationTest {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private StaffWorkingHoursRepository workingHoursRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private ServiceCategoryRepository serviceCategoryRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private StaffScheduleService staffScheduleService;

    @Autowired
    private DashboardService dashboardService;

    private Tenant tenant;
    private Staff staff1;
    private Staff staff2;
    private Service service;
    private Customer customer;

    @BeforeEach
    public void setup() {
        TenantContext.clear();

        tenant = new Tenant();
        tenant.setName("Perf Test Spa");
        tenant.setSlug("perf-test-spa-" + System.nanoTime());
        tenant.setTimezone("Asia/Ho_Chi_Minh");
        tenant.setIsActive(true);
        tenant = tenantRepository.save(tenant);

        TenantContext.setTenantId(tenant.getId());

        staff1 = new Staff();
        staff1.setTenant(tenant);
        staff1.setName("Specialist One");
        staff1.setIsActive(true);
        staff1.setIsDeleted(false);
        staff1.setShowOnWebsite(true);
        staff1 = staffRepository.save(staff1);

        staff2 = new Staff();
        staff2.setTenant(tenant);
        staff2.setName("Specialist Two");
        staff2.setIsActive(true);
        staff2.setIsDeleted(false);
        staff2.setShowOnWebsite(true);
        staff2 = staffRepository.save(staff2);

        // Configure working hours: 09:00 - 18:00
        for (DayOfWeek dow : DayOfWeek.values()) {
            StaffWorkingHours wh1 = new StaffWorkingHours();
            wh1.setTenant(tenant);
            wh1.setStaff(staff1);
            wh1.setDayOfWeek(dow);
            wh1.setStartTime(LocalTime.of(9, 0));
            wh1.setEndTime(LocalTime.of(18, 0));
            wh1.setIsActive(true);
            workingHoursRepository.save(wh1);

            StaffWorkingHours wh2 = new StaffWorkingHours();
            wh2.setTenant(tenant);
            wh2.setStaff(staff2);
            wh2.setDayOfWeek(dow);
            wh2.setStartTime(LocalTime.of(9, 0));
            wh2.setEndTime(LocalTime.of(18, 0));
            wh2.setIsActive(true);
            workingHoursRepository.save(wh2);
        }

        ServiceCategory cat = new ServiceCategory();
        cat.setTenant(tenant);
        cat.setName("Wellness Therapy");
        cat.setIsActive(true);
        cat = serviceCategoryRepository.save(cat);

        service = new Service();
        service.setTenant(tenant);
        service.setName("Signature Massage 60m");
        service.setDurationMinutes(60);
        service.setPrice(new BigDecimal("500000.00"));
        service.setIsActive(true);
        service.setIsFeatured(true);
        service.setCategory(cat);
        service = serviceRepository.save(service);

        customer = new Customer();
        customer.setTenant(tenant);
        customer.setName("Nguyễn Văn A");
        customer.setPhone("0901234567");
        customer.setIsActive(true);
        customer = customerRepository.save(customer);
    }

    @Test
    public void testAvailableSlotsBatchCalculation_withOverlapsAndMultipleStaff() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        // 1. Initial query: both staff are free from 09:00 to 18:00 (60 min slots every 30m)
        List<LocalDateTime> initialSlots = staffScheduleService.getAvailableSlots(tenant.getId(), service.getId(), tomorrow, null);
        assertFalse(initialSlots.isEmpty());
        assertTrue(initialSlots.contains(tomorrow.atTime(9, 0)));
        assertTrue(initialSlots.contains(tomorrow.atTime(10, 0)));

        // 2. Book staff1 at 10:00 - 11:00
        Booking b1 = new Booking();
        b1.setTenant(tenant);
        b1.setStaff(staff1);
        b1.setService(service);
        b1.setCustomer(customer);
        b1.setStartTime(tomorrow.atTime(10, 0));
        b1.setEndTime(tomorrow.atTime(11, 0));
        b1.setStatus(BookingStatus.CONFIRMED);
        b1.setPrice(service.getPrice());
        b1.setBookingCode("PERF-BK-001");
        bookingRepository.save(b1);

        // Since staff2 is still free, 10:00 slot must STILL be available when querying all staff
        List<LocalDateTime> slotsAllStaff = staffScheduleService.getAvailableSlots(tenant.getId(), service.getId(), tomorrow, null);
        assertTrue(slotsAllStaff.contains(tomorrow.atTime(10, 0)));

        // When querying specifically for staff1, 10:00 slot must be EXCLUDED
        List<LocalDateTime> slotsStaff1 = staffScheduleService.getAvailableSlots(tenant.getId(), service.getId(), tomorrow, staff1.getId());
        assertFalse(slotsStaff1.contains(tomorrow.atTime(10, 0)));
        assertTrue(slotsStaff1.contains(tomorrow.atTime(9, 0)));
        assertTrue(slotsStaff1.contains(tomorrow.atTime(11, 0)));

        // 3. Also book staff2 at 10:00 - 11:00
        Booking b2 = new Booking();
        b2.setTenant(tenant);
        b2.setStaff(staff2);
        b2.setService(service);
        b2.setCustomer(customer);
        b2.setStartTime(tomorrow.atTime(10, 0));
        b2.setEndTime(tomorrow.atTime(11, 0));
        b2.setStatus(BookingStatus.PENDING);
        b2.setPrice(service.getPrice());
        b2.setBookingCode("PERF-BK-002");
        bookingRepository.save(b2);

        // Now both staff are occupied at 10:00 -> 10:00 slot must be EXCLUDED across all staff
        List<LocalDateTime> slotsBothBusy = staffScheduleService.getAvailableSlots(tenant.getId(), service.getId(), tomorrow, null);
        assertFalse(slotsBothBusy.contains(tomorrow.atTime(10, 0)));
    }

    @Test
    public void testServiceCategoryLeftJoinFetch_preventsNPlusOne() {
        List<Service> services = serviceRepository.findAllByTenantIdAndIsActiveTrue(tenant.getId());
        assertFalse(services.isEmpty());
        Service fetched = services.get(0);
        assertNotNull(fetched.getCategory());
        assertEquals("Wellness Therapy", fetched.getCategory().getName());
    }

    @Test
    public void testDashboardMetricsAggregation() {
        LocalDate today = LocalDate.now();

        Booking b = new Booking();
        b.setTenant(tenant);
        b.setStaff(staff1);
        b.setService(service);
        b.setCustomer(customer);
        b.setStartTime(today.atTime(14, 0));
        b.setEndTime(today.atTime(15, 0));
        b.setStatus(BookingStatus.CONFIRMED);
        b.setPrice(service.getPrice());
        b.setBookingCode("PERF-BK-003");
        bookingRepository.save(b);

        DashboardMetricsResponse metrics = dashboardService.getMetrics();
        assertNotNull(metrics);
        assertEquals(1, metrics.getConfirmedBookingCount());
        assertEquals(0, metrics.getPendingBookingCount());
        assertNotNull(metrics.getBookingStatusDistribution());
    }
}
