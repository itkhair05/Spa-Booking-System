package com.example.spabooking.review.controller;

import com.example.spabooking.review.dto.ReviewResponse;
import com.example.spabooking.review.dto.UpdateReviewRequest;
import com.example.spabooking.review.entity.Review;
import com.example.spabooking.review.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @Autowired
    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<ReviewResponse>> getAll() {
        List<Review> reviews = reviewService.findAll();
        List<ReviewResponse> response = reviews.stream()
                .map(ReviewResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ReviewResponse> getById(@PathVariable Long id) {
        Review review = reviewService.findById(id);
        return ResponseEntity.ok(ReviewResponse.fromEntity(review));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ReviewResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReviewRequest request) {
        Review updated = reviewService.update(id, request);
        return ResponseEntity.ok(ReviewResponse.fromEntity(updated));
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ReviewResponse> publish(@PathVariable Long id) {
        Review published = reviewService.publish(id);
        return ResponseEntity.ok(ReviewResponse.fromEntity(published));
    }

    @PatchMapping("/{id}/unpublish")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ReviewResponse> unpublish(@PathVariable Long id) {
        Review unpublished = reviewService.unpublish(id);
        return ResponseEntity.ok(ReviewResponse.fromEntity(unpublished));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reviewService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
