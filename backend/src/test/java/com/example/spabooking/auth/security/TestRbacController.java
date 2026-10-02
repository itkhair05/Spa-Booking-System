package com.example.spabooking.auth.security;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test-rbac")
public class TestRbacController {

    @GetMapping("/owner-only")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<String> ownerOnly() {
        return ResponseEntity.ok("owner-success");
    }

    @GetMapping("/staff-compatible")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<String> staffCompatible() {
        return ResponseEntity.ok("staff-success");
    }
    
    @GetMapping("/unprotected")
    public ResponseEntity<String> unprotected() {
        return ResponseEntity.ok("unprotected-success");
    }
}
