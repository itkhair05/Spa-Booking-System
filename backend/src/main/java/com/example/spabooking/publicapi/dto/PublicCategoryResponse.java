package com.example.spabooking.publicapi.dto;

import com.example.spabooking.service.entity.ServiceCategory;

public class PublicCategoryResponse {

    private Long id;
    private String name;
    private String description;
    private Integer displayOrder;

    public static PublicCategoryResponse fromEntity(ServiceCategory category) {
        PublicCategoryResponse resp = new PublicCategoryResponse();
        resp.setId(category.getId());
        resp.setName(category.getName());
        resp.setDescription(category.getDescription());
        resp.setDisplayOrder(category.getDisplayOrder());
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
}
