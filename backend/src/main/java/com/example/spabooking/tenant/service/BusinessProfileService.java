package com.example.spabooking.tenant.service;

import com.example.spabooking.common.exception.ResourceNotFoundException;
import com.example.spabooking.tenant.context.TenantContext;
import com.example.spabooking.tenant.dto.BusinessProfileResponse;
import com.example.spabooking.tenant.dto.UpdateBusinessProfileRequest;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessProfileService {

    private final TenantRepository tenantRepository;

    @Autowired
    public BusinessProfileService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public BusinessProfileResponse getProfile() {
        Long tenantId = TenantContext.requireTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        return BusinessProfileResponse.fromEntity(tenant);
    }

    @Transactional
    public BusinessProfileResponse updateProfile(UpdateBusinessProfileRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        tenant.setName(request.getName());
        tenant.setPhone(request.getPhone());
        tenant.setEmail(request.getEmail());
        tenant.setAddress(request.getAddress());
        if (request.getTimezone() != null && !request.getTimezone().isBlank()) {
            tenant.setTimezone(request.getTimezone());
        }

        Tenant updated = tenantRepository.save(tenant);
        return BusinessProfileResponse.fromEntity(updated);
    }
}
