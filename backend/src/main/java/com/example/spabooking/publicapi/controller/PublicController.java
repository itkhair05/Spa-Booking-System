package com.example.spabooking.publicapi.controller;

import com.example.spabooking.publicapi.dto.*;
import com.example.spabooking.publicapi.service.PublicBookingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/public/spas/{slug}")
public class PublicController {

    private final PublicBookingService publicBookingService;

    @Autowired
    public PublicController(PublicBookingService publicBookingService) {
        this.publicBookingService = publicBookingService;
    }

    @GetMapping
    public ResponseEntity<PublicSpaInfoResponse> getSpaInfo(@PathVariable String slug) {
        // slug is processed in PublicTenantContextFilter
        return ResponseEntity.ok(publicBookingService.getSpaInfo());
    }

    @GetMapping("/services")
    public ResponseEntity<List<PublicServiceResponse>> getServices(@PathVariable String slug) {
        return ResponseEntity.ok(publicBookingService.getActiveServices());
    }

    @GetMapping("/staff")
    public ResponseEntity<List<PublicStaffResponse>> getStaff(@PathVariable String slug) {
        return ResponseEntity.ok(publicBookingService.getActiveStaff());
    }

    @GetMapping("/availability")
    public ResponseEntity<List<LocalDateTime>> getAvailability(
            @PathVariable String slug,
            @RequestParam Long serviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long staffId) {
        return ResponseEntity.ok(publicBookingService.getAvailability(serviceId, date, staffId));
    }

    @PostMapping("/bookings")
    public ResponseEntity<PublicBookingResponse> createBooking(
            @PathVariable String slug,
            @Valid @RequestBody CreatePublicBookingRequest request) {
        PublicBookingResponse response = publicBookingService.createBooking(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/bookings/{bookingCode}")
    public ResponseEntity<PublicBookingDetailResponse> getBookingByCode(
            @PathVariable String slug,
            @PathVariable String bookingCode) {
        return ResponseEntity.ok(publicBookingService.getBookingByCode(bookingCode));
    }
}
