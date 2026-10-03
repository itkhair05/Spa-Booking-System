package com.example.spabooking.publicapi.dto;

import com.example.spabooking.service.entity.Service;
import java.math.BigDecimal;

public class PublicServiceResponse {
    private Long id;
    private String name;
    private String description;
    private Integer durationMinutes;
    private BigDecimal price;

    public static PublicServiceResponse fromEntity(Service service) {
        PublicServiceResponse resp = new PublicServiceResponse();
        resp.setId(service.getId());
        resp.setName(service.getName());
        resp.setDescription(service.getDescription());
        resp.setDurationMinutes(service.getDurationMinutes());
        resp.setPrice(service.getPrice());
        return resp;
    }

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
}
