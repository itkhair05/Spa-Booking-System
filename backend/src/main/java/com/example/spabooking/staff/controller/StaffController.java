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

import java.util.List;

@RestController
@RequestMapping("/api/v1/staff")
public class StaffController {

    private final StaffService staffService;

    @Autowired
    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<List<StaffResponse>> getAllStaff() {
        return ResponseEntity.ok(staffService.findAllWithAccounts());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
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
        if (request.getIsActive() != null) {
            staff.setIsActive(request.getIsActive());
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
        if (request.getIsActive() != null) {
            staffDetails.setIsActive(request.getIsActive());
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
}
