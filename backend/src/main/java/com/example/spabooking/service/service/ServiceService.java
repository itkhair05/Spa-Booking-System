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

    @Autowired
    public ServiceService(ServiceRepository serviceRepository, TenantRepository tenantRepository) {
        this.serviceRepository = serviceRepository;
        this.tenantRepository = tenantRepository;
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
        Long tenantId = TenantContext.requireTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        service.setTenant(tenant);
        return serviceRepository.save(service);
    }

    public Service update(Long id, Service updatedDetails) {
        Long tenantId = TenantContext.requireTenantId();
        Service existingService = serviceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        
        existingService.setName(updatedDetails.getName());
        existingService.setDescription(updatedDetails.getDescription());
        existingService.setDurationMinutes(updatedDetails.getDurationMinutes());
        existingService.setPrice(updatedDetails.getPrice());
        
        if (updatedDetails.getIsActive() != null) {
            existingService.setIsActive(updatedDetails.getIsActive());
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
