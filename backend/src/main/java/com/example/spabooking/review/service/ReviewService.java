package com.example.spabooking.review.service;

import com.example.spabooking.common.exception.ResourceNotFoundException;
import com.example.spabooking.review.dto.UpdateReviewRequest;
import com.example.spabooking.review.entity.Review;
import com.example.spabooking.review.repository.ReviewRepository;
import com.example.spabooking.tenant.context.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;

    @Autowired
    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public List<Review> findAll() {
        Long tenantId = TenantContext.requireTenantId();
        return reviewRepository.findAllByTenantIdOrderByDisplayOrderAscCreatedAtDesc(tenantId);
    }

    public List<Review> findAllPublished() {
        Long tenantId = TenantContext.requireTenantId();
        return reviewRepository.findAllByTenantIdAndIsPublishedTrueOrderByDisplayOrderAscCreatedAtDesc(tenantId);
    }

    public Review findById(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        return reviewRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));
    }

    @Transactional
    public Review update(Long id, UpdateReviewRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        Review review = reviewRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));

        if (request.getIsPublished() != null) {
            review.setIsPublished(request.getIsPublished());
        }
        if (request.getDisplayOrder() != null) {
            review.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getComment() != null && !request.getComment().isBlank()) {
            review.setComment(request.getComment().trim());
        }

        return reviewRepository.save(review);
    }

    @Transactional
    public Review publish(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        Review review = reviewRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));
        review.setIsPublished(true);
        return reviewRepository.save(review);
    }

    @Transactional
    public Review unpublish(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        Review review = reviewRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));
        review.setIsPublished(false);
        return reviewRepository.save(review);
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        Review review = reviewRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));
        reviewRepository.delete(review);
    }
}
