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
    private final com.example.spabooking.feedback.service.FeedbackService feedbackService;

    @Autowired
    public PublicController(PublicBookingService publicBookingService,
                            com.example.spabooking.feedback.service.FeedbackService feedbackService) {
        this.publicBookingService = publicBookingService;
        this.feedbackService = feedbackService;
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

    @GetMapping("/services/featured")
    public ResponseEntity<List<PublicServiceResponse>> getFeaturedServices(@PathVariable String slug) {
        return ResponseEntity.ok(publicBookingService.getFeaturedServices());
    }

    @GetMapping("/categories")
    public ResponseEntity<List<PublicCategoryResponse>> getCategories(@PathVariable String slug) {
        return ResponseEntity.ok(publicBookingService.getActiveCategories());
    }

    @GetMapping("/staff")
    public ResponseEntity<List<PublicStaffResponse>> getStaff(@PathVariable String slug) {
        return ResponseEntity.ok(publicBookingService.getActiveStaff());
    }

    @GetMapping("/articles")
    public ResponseEntity<List<com.example.spabooking.article.dto.PublicArticleResponse>> getArticles(@PathVariable String slug) {
        return ResponseEntity.ok(publicBookingService.getPublishedArticles());
    }

    @GetMapping("/articles/{slugOrId}")
    public ResponseEntity<com.example.spabooking.article.dto.PublicArticleResponse> getArticle(
            @PathVariable String slug,
            @PathVariable String slugOrId) {
        return ResponseEntity.ok(publicBookingService.getPublishedArticle(slugOrId));
    }

    @GetMapping("/reviews")
    public ResponseEntity<List<com.example.spabooking.review.dto.PublicReviewResponse>> getReviews(@PathVariable String slug) {
        return ResponseEntity.ok(publicBookingService.getPublishedReviews());
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

    @PostMapping("/feedback")
    public ResponseEntity<com.example.spabooking.feedback.dto.FeedbackResponse> submitFeedback(
            @PathVariable String slug,
            @Valid @RequestBody com.example.spabooking.feedback.dto.CreateFeedbackRequest request) {
        com.example.spabooking.feedback.dto.FeedbackResponse response = feedbackService.submitPublicFeedback(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/payments/vnpay-callback")
    public ResponseEntity<com.example.spabooking.payment.dto.VNPayCallbackResult> handleVNPayCallback(
            @PathVariable String slug,
            @RequestParam java.util.Map<String, String> allParams) {
        return ResponseEntity.ok(publicBookingService.processVNPayCallback(allParams));
    }
}
