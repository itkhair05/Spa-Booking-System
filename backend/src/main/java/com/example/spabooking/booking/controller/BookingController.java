package com.example.spabooking.booking.controller;

import com.example.spabooking.booking.dto.AssignBookingRequest;
import com.example.spabooking.booking.dto.BookingDetailResponse;
import com.example.spabooking.booking.dto.BookingResponse;
import com.example.spabooking.booking.dto.CreateBookingRequest;
import com.example.spabooking.booking.dto.UpdateBookingRequest;
import com.example.spabooking.booking.dto.UpdateBookingStatusRequest;
import com.example.spabooking.booking.entity.Booking;
import com.example.spabooking.booking.service.BookingService;
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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) com.example.spabooking.booking.enums.BookingStatus status) {
        
        List<Booking> bookings = bookingService.findAll(staffId, startDate, endDate, status);
        List<BookingResponse> response = bookings.stream()
                .map(BookingResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<BookingDetailResponse> getBookingById(@PathVariable Long id) {
        BookingDetailResponse detail = bookingService.getBookingDetail(id);
        return ResponseEntity.ok(detail);
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

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<BookingResponse> confirmBooking(@PathVariable Long id) {
        Booking updated = bookingService.confirm(id);
        return ResponseEntity.ok(BookingResponse.fromEntity(updated));
    }

    @PostMapping("/{id}/check-in")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<BookingResponse> checkInBooking(@PathVariable Long id) {
        Booking updated = bookingService.checkIn(id);
        return ResponseEntity.ok(BookingResponse.fromEntity(updated));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<BookingResponse> startBooking(@PathVariable Long id) {
        Booking updated = bookingService.start(id);
        return ResponseEntity.ok(BookingResponse.fromEntity(updated));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<BookingResponse> completeBooking(@PathVariable Long id) {
        Booking updated = bookingService.complete(id);
        return ResponseEntity.ok(BookingResponse.fromEntity(updated));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable Long id,
            @RequestBody(required = false) com.example.spabooking.booking.dto.CancelBookingRequest request) {
        Booking updated = bookingService.cancel(id, request != null ? request.getReason() : null);
        return ResponseEntity.ok(BookingResponse.fromEntity(updated));
    }

    @PostMapping("/{id}/no-show")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<BookingResponse> noShowBooking(@PathVariable Long id) {
        Booking updated = bookingService.noShow(id);
        return ResponseEntity.ok(BookingResponse.fromEntity(updated));
    }

    @PostMapping("/{id}/reschedule")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<BookingResponse> rescheduleBooking(
            @PathVariable Long id,
            @Valid @RequestBody com.example.spabooking.booking.dto.RescheduleBookingRequest request) {
        Booking updated = bookingService.reschedule(id, request);
        return ResponseEntity.ok(BookingResponse.fromEntity(updated));
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<BookingResponse> assignBooking(
            @PathVariable Long id,
            @Valid @RequestBody AssignBookingRequest request) {
        Booking updatedBooking = bookingService.assignStaff(id, request.getStaffId());
        return ResponseEntity.ok(BookingResponse.fromEntity(updatedBooking));
    }
}
