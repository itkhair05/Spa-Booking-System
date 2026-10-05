package com.example.spabooking.review.dto;

import com.example.spabooking.review.entity.Review;
import java.time.LocalDateTime;

public class PublicReviewResponse {

    private Long id;
    private String customerName;
    private Integer rating;
    private String comment;
    private String serviceName;
    private Boolean isDemo;
    private LocalDateTime createdAt;

    public static PublicReviewResponse fromEntity(Review review) {
        PublicReviewResponse resp = new PublicReviewResponse();
        resp.setId(review.getId());
        resp.setCustomerName(review.getCustomerName());
        resp.setRating(review.getRating());
        resp.setComment(review.getComment());
        resp.setServiceName(review.getServiceName());
        resp.setIsDemo(review.getIsDemo());
        resp.setCreatedAt(review.getCreatedAt());
        return resp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public Boolean getIsDemo() { return isDemo; }
    public void setIsDemo(Boolean isDemo) { this.isDemo = isDemo; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
