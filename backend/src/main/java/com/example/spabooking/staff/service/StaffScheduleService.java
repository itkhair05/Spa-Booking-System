package com.example.spabooking.staff.service;

import com.example.spabooking.booking.entity.Booking;
import com.example.spabooking.booking.repository.BookingRepository;
import com.example.spabooking.booking.exception.BookingConflictException;
import com.example.spabooking.common.exception.ResourceNotFoundException;
import com.example.spabooking.service.entity.Service;
import com.example.spabooking.service.repository.ServiceRepository;
import com.example.spabooking.staff.dto.CreateDayOffRequest;
import com.example.spabooking.staff.dto.DailyScheduleItemResponse;
import com.example.spabooking.staff.dto.StaffDayOffDto;
import com.example.spabooking.staff.dto.StaffWorkingHoursDto;
import com.example.spabooking.staff.dto.UpdateWorkingHoursRequest;
import com.example.spabooking.staff.entity.Staff;
import com.example.spabooking.staff.entity.StaffDayOff;
import com.example.spabooking.staff.entity.StaffWorkingHours;
import com.example.spabooking.staff.repository.StaffDayOffRepository;
import com.example.spabooking.staff.repository.StaffRepository;
import com.example.spabooking.staff.repository.StaffWorkingHoursRepository;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public class StaffScheduleService {

    private final StaffRepository staffRepository;
    private final StaffWorkingHoursRepository workingHoursRepository;
    private final StaffDayOffRepository dayOffRepository;
    private final BookingRepository bookingRepository;
    private final ServiceRepository serviceRepository;
    private final TenantRepository tenantRepository;

    @Autowired
    public StaffScheduleService(StaffRepository staffRepository,
                                StaffWorkingHoursRepository workingHoursRepository,
                                StaffDayOffRepository dayOffRepository,
                                BookingRepository bookingRepository,
                                ServiceRepository serviceRepository,
                                TenantRepository tenantRepository) {
        this.staffRepository = staffRepository;
        this.workingHoursRepository = workingHoursRepository;
        this.dayOffRepository = dayOffRepository;
        this.bookingRepository = bookingRepository;
        this.serviceRepository = serviceRepository;
        this.tenantRepository = tenantRepository;
    }

    // ==========================================
    // 1. Working Hours Management
    // ==========================================

    @Transactional
    public List<StaffWorkingHoursDto> getWorkingHours(Long tenantId, Long staffId) {
        Staff staff = staffRepository.findByIdAndTenantId(staffId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        List<StaffWorkingHours> list = workingHoursRepository.findByStaffIdAndTenantId(staffId, tenantId);
        if (list.isEmpty()) {
            list = initDefaultWorkingHours(staff);
        }

        // Ensure 7 days are represented in standard Monday-Sunday order
        Map<DayOfWeek, StaffWorkingHours> map = list.stream()
                .collect(Collectors.toMap(StaffWorkingHours::getDayOfWeek, h -> h, (a, b) -> a));

        List<StaffWorkingHoursDto> result = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            StaffWorkingHours entry = map.get(day);
            if (entry != null) {
                result.add(StaffWorkingHoursDto.fromEntity(entry));
            } else {
                // In case a day was missing, create default
                StaffWorkingHours created = new StaffWorkingHours(
                        staff, staff.getTenant(), day,
                        LocalTime.of(9, 0), LocalTime.of(18, 0),
                        true
                );
                created = workingHoursRepository.save(created);
                result.add(StaffWorkingHoursDto.fromEntity(created));
            }
        }
        return result;
    }

    @Transactional
    public List<StaffWorkingHoursDto> updateWorkingHours(Long tenantId, Long staffId, UpdateWorkingHoursRequest request) {
        Staff staff = staffRepository.findByIdAndTenantId(staffId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        if (request == null || request.getWorkingHours() == null || request.getWorkingHours().isEmpty()) {
            throw new IllegalArgumentException("Working hours list cannot be empty");
        }

        // Validate duplicates and time boundaries
        Set<DayOfWeek> seenDays = new HashSet<>();
        for (StaffWorkingHoursDto dto : request.getWorkingHours()) {
            if (dto.getDayOfWeek() == null) {
                throw new IllegalArgumentException("Day of week is required");
            }
            if (!seenDays.add(dto.getDayOfWeek())) {
                throw new IllegalArgumentException("Duplicate working hours for day: " + dto.getDayOfWeek());
            }
            if (dto.getStartTime() == null || dto.getEndTime() == null) {
                throw new IllegalArgumentException("Start time and end time are required for " + dto.getDayOfWeek());
            }
            if (!dto.getStartTime().isBefore(dto.getEndTime())) {
                throw new IllegalArgumentException("Start time must be before end time for " + dto.getDayOfWeek());
            }
        }

        Map<DayOfWeek, StaffWorkingHours> existingMap = workingHoursRepository.findByStaffIdAndTenantId(staffId, tenantId)
                .stream()
                .collect(Collectors.toMap(StaffWorkingHours::getDayOfWeek, h -> h, (a, b) -> a));

        List<StaffWorkingHours> toSave = new ArrayList<>();
        for (StaffWorkingHoursDto dto : request.getWorkingHours()) {
            StaffWorkingHours entity = existingMap.get(dto.getDayOfWeek());
            if (entity == null) {
                entity = new StaffWorkingHours();
                entity.setStaff(staff);
                entity.setTenant(staff.getTenant());
                entity.setDayOfWeek(dto.getDayOfWeek());
            }
            entity.setStartTime(dto.getStartTime());
            entity.setEndTime(dto.getEndTime());
            entity.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
            toSave.add(entity);
        }

        workingHoursRepository.saveAll(toSave);
        return getWorkingHours(tenantId, staffId);
    }

    @Transactional
    public List<StaffWorkingHours> initDefaultWorkingHours(Staff staff) {
        List<StaffWorkingHours> defaults = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            defaults.add(new StaffWorkingHours(
                    staff,
                    staff.getTenant(),
                    day,
                    LocalTime.of(9, 0),
                    LocalTime.of(18, 0),
                    true
            ));
        }
        return workingHoursRepository.saveAll(defaults);
    }

    // ==========================================
    // 2. Days Off Management
    // ==========================================

    @Transactional(readOnly = true)
    public List<StaffDayOffDto> getDaysOff(Long tenantId, Long staffId) {
        staffRepository.findByIdAndTenantId(staffId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        return dayOffRepository.findByStaffIdAndTenantIdOrderByDateAsc(staffId, tenantId)
                .stream()
                .map(StaffDayOffDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public StaffDayOffDto addDayOff(Long tenantId, Long staffId, CreateDayOffRequest request) {
        Staff staff = staffRepository.findByIdAndTenantId(staffId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        if (request == null || request.getDate() == null) {
            throw new IllegalArgumentException("Date is required");
        }

        if (dayOffRepository.existsByStaffIdAndTenantIdAndDate(staffId, tenantId, request.getDate())) {
            throw new IllegalArgumentException("Nhân viên đã có lịch nghỉ vào ngày " + request.getDate());
        }

        StaffDayOff dayOff = new StaffDayOff(staff, staff.getTenant(), request.getDate(), request.getReason());
        dayOff = dayOffRepository.save(dayOff);
        return StaffDayOffDto.fromEntity(dayOff);
    }

    @Transactional
    public void deleteDayOff(Long tenantId, Long staffId, Long dayOffId) {
        StaffDayOff dayOff = dayOffRepository.findByIdAndStaffIdAndTenantId(dayOffId, staffId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Day off record not found"));
        dayOffRepository.delete(dayOff);
    }

    // ==========================================
    // 3. Authoritative Availability Validation
    // ==========================================

    /**
     * Validates that the staff member is available for the given time interval.
     * Throws BookingConflictException or IllegalArgumentException if unavailable.
     */
    @Transactional
    public void validateStaffAvailability(Long tenantId, Long staffId, LocalDateTime startTime, LocalDateTime endTime, Long excludeBookingId) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Start time and end time are required");
        }
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        Staff staff = staffRepository.findByIdAndTenantId(staffId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        if (!Boolean.TRUE.equals(staff.getIsActive()) || Boolean.TRUE.equals(staff.getIsDeleted())) {
            throw new BookingConflictException("Nhân viên hiện không hoạt động");
        }

        LocalDate appointmentDate = startTime.toLocalDate();

        // 1. Day Off check
        if (dayOffRepository.existsByStaffIdAndTenantIdAndDate(staffId, tenantId, appointmentDate)) {
            throw new BookingConflictException("Nhân viên có lịch nghỉ vào ngày " + appointmentDate);
        }

        // 2. Working Hours check
        DayOfWeek dayOfWeek = startTime.getDayOfWeek();
        Optional<StaffWorkingHours> workingHoursOpt = workingHoursRepository.findByStaffIdAndTenantIdAndDayOfWeek(staffId, tenantId, dayOfWeek);
        if (workingHoursOpt.isEmpty() && workingHoursRepository.findByStaffIdAndTenantId(staffId, tenantId).isEmpty()) {
            List<StaffWorkingHours> defaults = initDefaultWorkingHours(staff);
            workingHoursOpt = defaults.stream().filter(h -> h.getDayOfWeek() == dayOfWeek).findFirst();
        }

        if (workingHoursOpt.isEmpty() || !Boolean.TRUE.equals(workingHoursOpt.get().getIsActive())) {
            throw new BookingConflictException("Nhân viên không làm việc vào ngày " + dayOfWeek);
        }

        StaffWorkingHours wh = workingHoursOpt.get();
        LocalTime requestedStartTime = startTime.toLocalTime();
        LocalTime requestedEndTime = endTime.toLocalTime();

        if (requestedStartTime.isBefore(wh.getStartTime())) {
            throw new BookingConflictException("Thời gian hẹn (" + requestedStartTime + ") trước giờ bắt đầu làm việc của nhân viên (" + wh.getStartTime() + ")");
        }

        // If endTime is on next day or past working hours
        if (!endTime.toLocalDate().isEqual(appointmentDate) || requestedEndTime.isAfter(wh.getEndTime())) {
            throw new BookingConflictException("Thời gian kết thúc dịch vụ (" + requestedEndTime + ") vượt quá giờ làm việc của nhân viên (" + wh.getEndTime() + ")");
        }

        // 3. Booking Overlap check (blocking statuses: PENDING, CONFIRMED, CHECKED_IN, IN_PROGRESS)
        long overlaps = bookingRepository.countOverlappingStaffBookings(tenantId, staffId, startTime, endTime, excludeBookingId);
        if (overlaps > 0) {
            throw new BookingConflictException("Nhân viên đã có lịch hẹn trùng giờ trong khoảng thời gian đã chọn");
        }
    }

    /**
     * Non-throwing version for availability checks.
     */
    @Transactional(readOnly = true)
    public boolean isStaffAvailable(Long tenantId, Long staffId, LocalDateTime startTime, LocalDateTime endTime, Long excludeBookingId) {
        try {
            validateStaffAvailability(tenantId, staffId, startTime, endTime, excludeBookingId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ==========================================
    // 4. Time Slot Generation
    // ==========================================

    @Transactional
    public List<LocalDateTime> getAvailableSlots(Long tenantId, Long serviceId, LocalDate date, Long staffId) {
        Service service = serviceRepository.findByIdAndTenantId(serviceId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        if (!Boolean.TRUE.equals(service.getIsActive())) {
            return Collections.emptyList();
        }

        int durationMinutes = service.getDurationMinutes();
        LocalDateTime now = LocalDateTime.now();

        List<Staff> candidateStaff = new ArrayList<>();
        if (staffId != null) {
            Staff staff = staffRepository.findByIdAndTenantId(staffId, tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));
            if (Boolean.TRUE.equals(staff.getIsActive()) && !Boolean.TRUE.equals(staff.getIsDeleted())) {
                candidateStaff.add(staff);
            }
        } else {
            candidateStaff = staffRepository.findAllByTenantIdAndIsActiveTrue(tenantId).stream()
                    .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                    .collect(Collectors.toList());
        }

        if (candidateStaff.isEmpty()) {
            return Collections.emptyList();
        }

        // Determine earliest open and latest close across working candidate staff for this day
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        Map<Long, StaffWorkingHours> staffWorkingHoursMap = new HashMap<>();
        List<Staff> workingStaff = new ArrayList<>();

        for (Staff s : candidateStaff) {
            if (dayOffRepository.existsByStaffIdAndTenantIdAndDate(s.getId(), tenantId, date)) {
                continue;
            }
            Optional<StaffWorkingHours> whOpt = workingHoursRepository.findByStaffIdAndTenantIdAndDayOfWeek(s.getId(), tenantId, dayOfWeek);
            if (whOpt.isEmpty() && workingHoursRepository.findByStaffIdAndTenantId(s.getId(), tenantId).isEmpty()) {
                List<StaffWorkingHours> defaults = initDefaultWorkingHours(s);
                whOpt = defaults.stream().filter(h -> h.getDayOfWeek() == dayOfWeek).findFirst();
            }
            if (whOpt.isPresent() && Boolean.TRUE.equals(whOpt.get().getIsActive())) {
                staffWorkingHoursMap.put(s.getId(), whOpt.get());
                workingStaff.add(s);
            }
        }

        if (staffWorkingHoursMap.isEmpty() || workingStaff.isEmpty()) {
            return Collections.emptyList();
        }

        LocalTime earliestOpen = staffWorkingHoursMap.values().stream()
                .map(StaffWorkingHours::getStartTime).min(LocalTime::compareTo).orElse(LocalTime.of(9, 0));
        LocalTime latestClose = staffWorkingHoursMap.values().stream()
                .map(StaffWorkingHours::getEndTime).max(LocalTime::compareTo).orElse(LocalTime.of(18, 0));

        LocalDateTime currentSlot = date.atTime(earliestOpen);
        LocalDateTime endOfDay = date.atTime(latestClose);

        // Pre-fetch all blocking bookings for working staff on this date in a single batch query
        List<Long> workingStaffIds = workingStaff.stream().map(Staff::getId).collect(Collectors.toList());
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
        List<Booking> blockingBookings = bookingRepository.findBlockingBookingsForStaff(tenantId, workingStaffIds, dayStart, dayEnd);
        Map<Long, List<Booking>> bookingsByStaff = blockingBookings.stream()
                .filter(b -> b.getStaff() != null)
                .collect(Collectors.groupingBy(b -> b.getStaff().getId()));

        List<LocalDateTime> availableSlots = new ArrayList<>();

        while (!currentSlot.plusMinutes(durationMinutes).isAfter(endOfDay)) {
            LocalDateTime slotEnd = currentSlot.plusMinutes(durationMinutes);

            // Skip past slots for today
            if (date.isEqual(now.toLocalDate()) && currentSlot.isBefore(now)) {
                currentSlot = currentSlot.plusMinutes(30);
                continue;
            }

            LocalTime slotStartTime = currentSlot.toLocalTime();
            LocalTime slotEndTime = slotEnd.toLocalTime();
            boolean isSameDay = slotEnd.toLocalDate().isEqual(date);

            // Check if at least one working staff member is available for this slot in-memory
            boolean anyStaffAvailable = false;
            for (Staff s : workingStaff) {
                StaffWorkingHours wh = staffWorkingHoursMap.get(s.getId());
                if (wh == null) continue;

                // Check staff working hours bounds
                if (slotStartTime.isBefore(wh.getStartTime()) || !isSameDay || slotEndTime.isAfter(wh.getEndTime())) {
                    continue;
                }

                // Check overlap against pre-fetched bookings for this staff member
                List<Booking> staffBookings = bookingsByStaff.getOrDefault(s.getId(), Collections.emptyList());
                boolean hasOverlap = false;
                for (Booking b : staffBookings) {
                    if (b.getStartTime().isBefore(slotEnd) && b.getEndTime().isAfter(currentSlot)) {
                        hasOverlap = true;
                        break;
                    }
                }

                if (!hasOverlap) {
                    anyStaffAvailable = true;
                    break;
                }
            }

            if (anyStaffAvailable) {
                availableSlots.add(currentSlot);
            }

            currentSlot = currentSlot.plusMinutes(30);
        }

        return availableSlots;
    }

    // ==========================================
    // 5. Daily Schedule
    // ==========================================

    @Transactional(readOnly = true)
    public List<DailyScheduleItemResponse> getDailySchedule(Long tenantId, LocalDate date, Long staffId) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        List<Booking> bookings = bookingRepository.findDailySchedule(tenantId, startOfDay, endOfDay, staffId);
        return bookings.stream()
                .map(DailyScheduleItemResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
