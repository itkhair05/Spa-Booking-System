package com.example.spabooking.booking.controller;

import com.example.spabooking.booking.dto.CreateBookingRequest;
import com.example.spabooking.booking.dto.BookingResponse;
import com.example.spabooking.booking.dto.UpdateBookingRequest;
import com.example.spabooking.booking.dto.UpdateBookingStatusRequest;
import com.example.spabooking.booking.entity.Booking;
import com.example.spabooking.booking.service.BookingService;
import com.example.spabooking.common.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;

    @Autowired
    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<List<BookingResponse>> getBookings(
            @RequestParam(required = false) Long staffId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        List<Booking> bookings = bookingService.findAll(staffId, startDate, endDate);
        List<BookingResponse> response = bookings.stream()
                .map(BookingResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable Long id) {
        Booking booking = bookingService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        return ResponseEntity.ok(BookingResponse.fromEntity(booking));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody CreateBookingRequest request) {
        Booking createdBooking = bookingService.create(request);
        return new ResponseEntity<>(BookingResponse.fromEntity(createdBooking), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<BookingResponse> updateBooking(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookingRequest request) {
        Booking updatedBooking = bookingService.update(id, request);
        return ResponseEntity.ok(BookingResponse.fromEntity(updatedBooking));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<BookingResponse> updateBookingStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookingStatusRequest request) {
        Booking updatedBooking = bookingService.updateStatus(id, request);
        return ResponseEntity.ok(BookingResponse.fromEntity(updatedBooking));
    }
}
