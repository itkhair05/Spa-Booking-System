package com.example.spabooking.service.dto;

import jakarta.validation.constraints.Size;

public class UpdateServiceCategoryRequest {

    @Size(max = 255, message = "Tên danh mục không được vượt quá 255 ký tự")
    private String name;

    private String description;

    private Integer displayOrder;

    private Boolean isActive;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }
}
