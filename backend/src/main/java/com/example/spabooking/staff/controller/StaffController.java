package com.example.spabooking.staff.controller;

import com.example.spabooking.staff.dto.CreateStaffAccountRequest;
import com.example.spabooking.staff.dto.CreateStaffRequest;
import com.example.spabooking.staff.dto.StaffAccountResponse;
import com.example.spabooking.staff.dto.StaffResponse;
import com.example.spabooking.staff.dto.UpdateStaffRequest;
import com.example.spabooking.staff.entity.Staff;
import com.example.spabooking.staff.service.StaffService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.spabooking.auth.security.CustomUserDetails;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/staff")
public class StaffController {

    private final StaffService staffService;
    private final com.example.spabooking.common.storage.FileStorageService fileStorageService;

    @Autowired
    public StaffController(StaffService staffService,
                           com.example.spabooking.common.storage.FileStorageService fileStorageService) {
        this.staffService = staffService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<StaffResponse>> getAllStaff() {
        return ResponseEntity.ok(staffService.findAllWithAccounts());
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<StaffResponse> getMyProfile() {
        Long staffId = getCurrentStaffId();
        return ResponseEntity.ok(staffService.findResponseById(staffId));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<StaffResponse> updateMyProfile(
            @Valid @RequestBody com.example.spabooking.staff.dto.UpdateStaffSelfProfileRequest request) {
        Long staffId = getCurrentStaffId();
        return ResponseEntity.ok(staffService.updateMyProfile(staffId, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<StaffResponse> getStaffById(@PathVariable Long id) {
        return ResponseEntity.ok(staffService.findResponseById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<StaffResponse> createStaff(@Valid @RequestBody CreateStaffRequest request) {
        Staff staff = new Staff();
        staff.setName(request.getName());
        staff.setPhone(request.getPhone());
        staff.setEmail(request.getEmail());
        staff.setAvatarUrl(request.getAvatarUrl());
        if (request.getIsActive() != null) {
            staff.setIsActive(request.getIsActive());
        }
        if (request.getShowOnWebsite() != null) {
            staff.setShowOnWebsite(request.getShowOnWebsite());
        }

        Staff createdStaff = staffService.create(staff);
        return new ResponseEntity<>(staffService.toResponse(createdStaff), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<StaffResponse> updateStaff(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStaffRequest request) {
        Staff staffDetails = new Staff();
        staffDetails.setName(request.getName());
        staffDetails.setPhone(request.getPhone());
        staffDetails.setEmail(request.getEmail());
        staffDetails.setAvatarUrl(request.getAvatarUrl());
        if (request.getIsActive() != null) {
            staffDetails.setIsActive(request.getIsActive());
        }
        if (request.getShowOnWebsite() != null) {
            staffDetails.setShowOnWebsite(request.getShowOnWebsite());
        }

        Staff updatedStaff = staffService.update(id, staffDetails);
        return ResponseEntity.ok(staffService.toResponse(updatedStaff));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> deleteStaff(@PathVariable Long id) {
        staffService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/account")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<StaffAccountResponse> createStaffAccount(
            @PathVariable Long id,
            @Valid @RequestBody CreateStaffAccountRequest request) {
        StaffAccountResponse response = staffService.createStaffAccount(id, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/me/avatar")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<StaffResponse> uploadMyAvatar(@RequestParam("file") MultipartFile file) {
        Long staffId = getCurrentStaffId();
        String fileUrl = fileStorageService.storeFile(file, "avatars");

        Staff staff = staffService.findById(staffId)
                .orElseThrow(() -> new AccessDeniedException("Không tìm thấy thông tin nhân viên"));
        staff.setAvatarUrl(fileUrl);
        Staff updatedStaff = staffService.update(staffId, staff);

        return ResponseEntity.ok(staffService.toResponse(updatedStaff));
    }

    @DeleteMapping("/me/avatar")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<Void> deleteMyAvatar() {
        Long staffId = getCurrentStaffId();
        Staff staff = staffService.findById(staffId)
                .orElseThrow(() -> new AccessDeniedException("Không tìm thấy thông tin nhân viên"));
        if (staff.getAvatarUrl() != null) {
            fileStorageService.deleteFileByUrl(staff.getAvatarUrl());
            staff.setAvatarUrl(null);
            staffService.update(staffId, staff);
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/avatar")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<StaffResponse> uploadStaffAvatar(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        String fileUrl = fileStorageService.storeFile(file, "avatars");
        Staff staff = staffService.findById(id)
                .orElseThrow(() -> new com.example.spabooking.common.exception.ResourceNotFoundException("Staff not found"));
        staff.setAvatarUrl(fileUrl);
        Staff updatedStaff = staffService.update(id, staff);
        return ResponseEntity.ok(staffService.toResponse(updatedStaff));
    }

    @DeleteMapping("/{id}/avatar")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> deleteStaffAvatar(@PathVariable Long id) {
        Staff staff = staffService.findById(id)
                .orElseThrow(() -> new com.example.spabooking.common.exception.ResourceNotFoundException("Staff not found"));
        if (staff.getAvatarUrl() != null) {
            fileStorageService.deleteFileByUrl(staff.getAvatarUrl());
            staff.setAvatarUrl(null);
            staffService.update(id, staff);
        }
        return ResponseEntity.noContent().build();
    }

    private Long getCurrentStaffId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            if (userDetails.isStaff() && userDetails.getStaffId() != null) {
                return userDetails.getStaffId();
            }
        }
        throw new AccessDeniedException("Chỉ tài khoản nhân viên mới có thể thực hiện thao tác này");
    }
}
