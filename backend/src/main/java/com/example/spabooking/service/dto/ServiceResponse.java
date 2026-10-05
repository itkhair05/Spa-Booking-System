package com.example.spabooking.service.dto;

import com.example.spabooking.service.entity.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ServiceResponse {

    private Long id;
    private String name;
    private String description;
    private Integer durationMinutes;
    private BigDecimal price;
    private Boolean isActive;
    private String imageUrl;
    private Long categoryId;
    private String categoryName;
    private Boolean isFeatured;
    private String processSteps;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ServiceResponse fromEntity(Service service) {
        ServiceResponse response = new ServiceResponse();
        response.setId(service.getId());
        response.setName(service.getName());
        response.setDescription(service.getDescription());
        response.setDurationMinutes(service.getDurationMinutes());
        response.setPrice(service.getPrice());
        response.setIsActive(service.getIsActive());
        response.setImageUrl(service.getImageUrl());
        if (service.getCategory() != null) {
            response.setCategoryId(service.getCategory().getId());
            response.setCategoryName(service.getCategory().getName());
        }
        response.setIsFeatured(service.getIsFeatured());
        response.setProcessSteps(service.getProcessSteps());
        response.setCreatedAt(service.getCreatedAt());
        response.setUpdatedAt(service.getUpdatedAt());
        return response;
    }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public Boolean getIsFeatured() { return isFeatured; }
    public void setIsFeatured(Boolean isFeatured) { this.isFeatured = isFeatured; }

    public String getProcessSteps() { return processSteps; }
    public void setProcessSteps(String processSteps) { this.processSteps = processSteps; }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
