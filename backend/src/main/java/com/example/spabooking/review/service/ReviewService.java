package com.example.spabooking.review.service;

import com.example.spabooking.common.exception.ResourceNotFoundException;
import com.example.spabooking.review.dto.CreateReviewRequest;
import com.example.spabooking.review.dto.UpdateReviewRequest;
import com.example.spabooking.review.entity.Review;
import com.example.spabooking.review.repository.ReviewRepository;
import com.example.spabooking.tenant.context.TenantContext;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final TenantRepository tenantRepository;

    @Autowired
    public ReviewService(ReviewRepository reviewRepository, TenantRepository tenantRepository) {
        this.reviewRepository = reviewRepository;
        this.tenantRepository = tenantRepository;
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
    public Review create(CreateReviewRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with id: " + tenantId));

        Review review = new Review();
        review.setTenant(tenant);
        review.setCustomerName(request.getCustomerName().trim());
        review.setRating(request.getRating() != null ? request.getRating() : 5);
        review.setComment(request.getComment().trim());
        review.setServiceName(request.getServiceName() != null && !request.getServiceName().isBlank()
                ? request.getServiceName().trim()
                : null);
        review.setIsPublished(request.getIsPublished() != null ? request.getIsPublished() : false);
        review.setIsDemo(request.getIsDemo() != null ? request.getIsDemo() : false);
        review.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);

        return reviewRepository.save(review);
    }

    @Transactional
    public Review update(Long id, UpdateReviewRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        Review review = reviewRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));

        if (request.getCustomerName() != null && !request.getCustomerName().isBlank()) {
            review.setCustomerName(request.getCustomerName().trim());
        }
        if (request.getRating() != null) {
            review.setRating(request.getRating());
        }
        if (request.getComment() != null && !request.getComment().isBlank()) {
            review.setComment(request.getComment().trim());
        }
        if (request.getServiceName() != null) {
            review.setServiceName(request.getServiceName().trim().isEmpty() ? null : request.getServiceName().trim());
        }
        if (request.getIsPublished() != null) {
            review.setIsPublished(request.getIsPublished());
        }
        if (request.getIsDemo() != null) {
            review.setIsDemo(request.getIsDemo());
        }
        if (request.getDisplayOrder() != null) {
            review.setDisplayOrder(request.getDisplayOrder());
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
