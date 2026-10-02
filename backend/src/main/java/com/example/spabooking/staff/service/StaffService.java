package com.example.spabooking.staff.service;

import com.example.spabooking.staff.entity.Staff;
import com.example.spabooking.staff.repository.StaffRepository;
import com.example.spabooking.tenant.context.TenantContext;
import com.example.spabooking.common.exception.ResourceNotFoundException;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StaffService {

    private final StaffRepository staffRepository;
    private final TenantRepository tenantRepository;

    @Autowired
    public StaffService(StaffRepository staffRepository, TenantRepository tenantRepository) {
        this.staffRepository = staffRepository;
        this.tenantRepository = tenantRepository;
    }

    public List<Staff> findAll() {
        Long tenantId = TenantContext.requireTenantId();
        return staffRepository.findAllByTenantIdAndIsActiveTrue(tenantId);
    }

    public Optional<Staff> findById(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        return staffRepository.findByIdAndTenantIdAndIsActiveTrue(id, tenantId);
    }

    public Staff create(Staff staff) {
        Long tenantId = TenantContext.requireTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        staff.setTenant(tenant);
        return staffRepository.save(staff);
    }

    public Staff update(Long id, Staff updatedDetails) {
        Long tenantId = TenantContext.requireTenantId();
        Staff existingStaff = staffRepository.findByIdAndTenantIdAndIsActiveTrue(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        existingStaff.setName(updatedDetails.getName());
        existingStaff.setPhone(updatedDetails.getPhone());
        existingStaff.setEmail(updatedDetails.getEmail());

        if (updatedDetails.getIsActive() != null) {
            existingStaff.setIsActive(updatedDetails.getIsActive());
        }

        return staffRepository.save(existingStaff);
    }

    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        Staff existingStaff = staffRepository.findByIdAndTenantIdAndIsActiveTrue(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        existingStaff.setIsActive(false);
        staffRepository.save(existingStaff);
    }
}
