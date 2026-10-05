package com.example.spabooking.service.service;

import com.example.spabooking.service.entity.Service;
import com.example.spabooking.service.repository.ServiceRepository;
import com.example.spabooking.tenant.context.TenantContext;
import com.example.spabooking.common.exception.ResourceNotFoundException;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Service
public class ServiceService {

    private final ServiceRepository serviceRepository;
    private final TenantRepository tenantRepository;
    private final com.example.spabooking.service.repository.ServiceCategoryRepository serviceCategoryRepository;

    @Autowired
    public ServiceService(ServiceRepository serviceRepository,
                          TenantRepository tenantRepository,
                          com.example.spabooking.service.repository.ServiceCategoryRepository serviceCategoryRepository) {
        this.serviceRepository = serviceRepository;
        this.tenantRepository = tenantRepository;
        this.serviceCategoryRepository = serviceCategoryRepository;
    }

    public List<Service> findAll() {
        Long tenantId = TenantContext.requireTenantId();
        return serviceRepository.findAllByTenantId(tenantId);
    }

    public Optional<Service> findById(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        return serviceRepository.findByIdAndTenantId(id, tenantId);
    }

    public Service create(Service service) {
        return create(service, null);
    }

    public Service create(Service service, Long categoryId) {
        Long tenantId = TenantContext.requireTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        service.setTenant(tenant);

        if (categoryId != null && categoryId > 0) {
            com.example.spabooking.service.entity.ServiceCategory category = serviceCategoryRepository
                    .findByIdAndTenantId(categoryId, tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
            service.setCategory(category);
        }

        return serviceRepository.save(service);
    }

    public Service update(Long id, Service updatedDetails) {
        return update(id, updatedDetails, null);
    }

    public Service update(Long id, Service updatedDetails, Long categoryId) {
        Long tenantId = TenantContext.requireTenantId();
        Service existingService = serviceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        
        existingService.setName(updatedDetails.getName());
        existingService.setDescription(updatedDetails.getDescription());
        existingService.setDurationMinutes(updatedDetails.getDurationMinutes());
        existingService.setPrice(updatedDetails.getPrice());
        if (updatedDetails.getImageUrl() != null) {
            existingService.setImageUrl(updatedDetails.getImageUrl());
        }
        
        if (updatedDetails.getIsActive() != null) {
            existingService.setIsActive(updatedDetails.getIsActive());
        }

        if (updatedDetails.getIsFeatured() != null) {
            existingService.setIsFeatured(updatedDetails.getIsFeatured());
        }

        if (updatedDetails.getProcessSteps() != null) {
            existingService.setProcessSteps(updatedDetails.getProcessSteps());
        }

        if (categoryId != null) {
            if (categoryId <= 0) {
                existingService.setCategory(null);
            } else {
                com.example.spabooking.service.entity.ServiceCategory category = serviceCategoryRepository
                        .findByIdAndTenantId(categoryId, tenantId)
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
                existingService.setCategory(category);
            }
        }
        
        return serviceRepository.save(existingService);
    }

    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        Service existingService = serviceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        
        serviceRepository.delete(existingService);
    }
}
