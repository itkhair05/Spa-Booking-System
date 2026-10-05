package com.example.spabooking.service.dto;

import com.example.spabooking.service.entity.ServiceCategory;

public class ServiceCategoryResponse {
    private Long id;
    private String name;
    private String description;
    private Integer displayOrder;
    private Boolean isActive;

    public static ServiceCategoryResponse fromEntity(ServiceCategory entity) {
        if (entity == null) return null;
        ServiceCategoryResponse resp = new ServiceCategoryResponse();
        resp.setId(entity.getId());
        resp.setName(entity.getName());
        resp.setDescription(entity.getDescription());
        resp.setDisplayOrder(entity.getDisplayOrder());
        resp.setIsActive(entity.getIsActive());
        return resp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }
}
