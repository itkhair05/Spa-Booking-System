package com.example.spabooking.service.service;

import com.example.spabooking.service.entity.Service;
import com.example.spabooking.service.repository.ServiceRepository;
import com.example.spabooking.tenant.context.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Service
public class ServiceService {

    private final ServiceRepository serviceRepository;

    @Autowired
    public ServiceService(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    public List<Service> findAll() {
        Long tenantId = TenantContext.requireTenantId();
        return serviceRepository.findAllByTenantId(tenantId);
    }

    public Optional<Service> findById(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        return serviceRepository.findByIdAndTenantId(id, tenantId);
    }
}
