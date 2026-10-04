package com.example.spabooking.feedback.service;

import com.example.spabooking.common.exception.ResourceNotFoundException;
import com.example.spabooking.feedback.dto.CreateFeedbackRequest;
import com.example.spabooking.feedback.dto.FeedbackResponse;
import com.example.spabooking.feedback.entity.Feedback;
import com.example.spabooking.feedback.entity.FeedbackStatus;
import com.example.spabooking.feedback.repository.FeedbackRepository;
import com.example.spabooking.tenant.context.TenantContext;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final TenantRepository tenantRepository;

    @Autowired
    public FeedbackService(FeedbackRepository feedbackRepository, TenantRepository tenantRepository) {
        this.feedbackRepository = feedbackRepository;
        this.tenantRepository = tenantRepository;
    }

    public FeedbackResponse submitPublicFeedback(CreateFeedbackRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        Feedback feedback = new Feedback();
        feedback.setTenant(tenant);
        feedback.setName(request.getName().trim());
        feedback.setPhone(request.getPhone().trim());
        feedback.setEmail(request.getEmail() != null ? request.getEmail().trim() : null);
        feedback.setType(request.getType());
        feedback.setBookingCode(request.getBookingCode() != null && !request.getBookingCode().trim().isEmpty()
                ? request.getBookingCode().trim() : null);
        feedback.setMessage(request.getMessage().trim());
        feedback.setStatus(FeedbackStatus.NEW);

        Feedback saved = feedbackRepository.save(feedback);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> findAllForCurrentTenant() {
        Long tenantId = TenantContext.requireTenantId();
        return feedbackRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public FeedbackResponse updateStatus(Long id, FeedbackStatus status) {
        Long tenantId = TenantContext.requireTenantId();
        Feedback feedback = feedbackRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phản hồi với mã ID: " + id));

        feedback.setStatus(status);
        Feedback updated = feedbackRepository.save(feedback);
        return toResponse(updated);
    }

    public FeedbackResponse toResponse(Feedback feedback) {
        return new FeedbackResponse(
                feedback.getId(),
                feedback.getName(),
                feedback.getPhone(),
                feedback.getEmail(),
                feedback.getType(),
                feedback.getBookingCode(),
                feedback.getMessage(),
                feedback.getStatus(),
                feedback.getCreatedAt(),
                feedback.getUpdatedAt()
        );
    }
}
