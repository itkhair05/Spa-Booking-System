package com.example.spabooking.tenant.controller;

import com.example.spabooking.tenant.dto.BusinessProfileResponse;
import com.example.spabooking.tenant.dto.UpdateBusinessProfileRequest;
import com.example.spabooking.tenant.service.BusinessProfileService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/business-profile")
public class BusinessProfileController {

    private final BusinessProfileService businessProfileService;

    @Autowired
    public BusinessProfileController(BusinessProfileService businessProfileService) {
        this.businessProfileService = businessProfileService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<BusinessProfileResponse> getProfile() {
        return ResponseEntity.ok(businessProfileService.getProfile());
    }

    @PutMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<BusinessProfileResponse> updateProfile(@Valid @RequestBody UpdateBusinessProfileRequest request) {
        return ResponseEntity.ok(businessProfileService.updateProfile(request));
    }
}
