package com.example.spabooking.feedback.controller;

import com.example.spabooking.feedback.dto.FeedbackResponse;
import com.example.spabooking.feedback.dto.UpdateFeedbackStatusRequest;
import com.example.spabooking.feedback.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/feedback")
@PreAuthorize("hasRole('OWNER')")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @Autowired
    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping
    public ResponseEntity<List<FeedbackResponse>> getAllFeedback() {
        return ResponseEntity.ok(feedbackService.findAllForCurrentTenant());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<FeedbackResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateFeedbackStatusRequest request) {
        return ResponseEntity.ok(feedbackService.updateStatus(id, request.getStatus()));
    }
}
