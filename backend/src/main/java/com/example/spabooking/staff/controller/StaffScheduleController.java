package com.example.spabooking.staff.controller;

import com.example.spabooking.auth.security.CustomUserDetails;
import com.example.spabooking.tenant.context.TenantContext;
import com.example.spabooking.staff.dto.CreateDayOffRequest;
import com.example.spabooking.staff.dto.DailyScheduleItemResponse;
import com.example.spabooking.staff.dto.StaffDayOffDto;
import com.example.spabooking.staff.dto.StaffWorkingHoursDto;
import com.example.spabooking.staff.dto.UpdateWorkingHoursRequest;
import com.example.spabooking.staff.service.StaffScheduleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class StaffScheduleController {

    private final StaffScheduleService scheduleService;

    @Autowired
    public StaffScheduleController(StaffScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    // ==========================================
    // 1. Working Hours
    // ==========================================

    @GetMapping("/staff/{staffId}/working-hours")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<List<StaffWorkingHoursDto>> getWorkingHours(@PathVariable Long staffId) {
        Long tenantId = TenantContext.requireTenantId();
        checkStaffAccess(staffId);
        return ResponseEntity.ok(scheduleService.getWorkingHours(tenantId, staffId));
    }

    @PutMapping("/staff/{staffId}/working-hours")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<StaffWorkingHoursDto>> updateWorkingHours(
            @PathVariable Long staffId,
            @Valid @RequestBody UpdateWorkingHoursRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(scheduleService.updateWorkingHours(tenantId, staffId, request));
    }

    // ==========================================
    // 2. Days Off
    // ==========================================

    @GetMapping("/staff/{staffId}/days-off")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<List<StaffDayOffDto>> getDaysOff(@PathVariable Long staffId) {
        Long tenantId = TenantContext.requireTenantId();
        checkStaffAccess(staffId);
        return ResponseEntity.ok(scheduleService.getDaysOff(tenantId, staffId));
    }

    @PostMapping("/staff/{staffId}/days-off")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<StaffDayOffDto> addDayOff(
            @PathVariable Long staffId,
            @Valid @RequestBody CreateDayOffRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        StaffDayOffDto response = scheduleService.addDayOff(tenantId, staffId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/staff/{staffId}/days-off/{dayOffId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> deleteDayOff(
            @PathVariable Long staffId,
            @PathVariable Long dayOffId) {
        Long tenantId = TenantContext.requireTenantId();
        scheduleService.deleteDayOff(tenantId, staffId, dayOffId);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // 3. Staff Availability Endpoint
    // ==========================================

    @GetMapping("/staff/{staffId}/availability")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<List<LocalDateTime>> getStaffAvailability(
            @PathVariable Long staffId,
            @RequestParam Long serviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Long tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(scheduleService.getAvailableSlots(tenantId, serviceId, date, staffId));
    }

    // ==========================================
    // 4. Daily Schedule
    // ==========================================

    @GetMapping("/schedule/daily")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<List<DailyScheduleItemResponse>> getDailySchedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long staffId) {
        Long tenantId = TenantContext.requireTenantId();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            if (userDetails.isStaff()) {
                // STAFF is strictly forced to view only their own daily schedule
                Long currentStaffId = userDetails.getStaffId();
                if (currentStaffId == null) {
                    throw new AccessDeniedException("Staff account is not linked to a staff record");
                }
                return ResponseEntity.ok(scheduleService.getDailySchedule(tenantId, date, currentStaffId));
            }
        }

        // OWNER can view all or filter by staffId
        return ResponseEntity.ok(scheduleService.getDailySchedule(tenantId, date, staffId));
    }

    private void checkStaffAccess(Long targetStaffId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            if (userDetails.isStaff()) {
                Long currentStaffId = userDetails.getStaffId();
                if (currentStaffId == null || !currentStaffId.equals(targetStaffId)) {
                    throw new AccessDeniedException("Nhân viên chỉ có thể xem lịch trình của chính mình");
                }
            }
        }
    }
}
