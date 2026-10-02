package com.example.spabooking.staff.service;

import com.example.spabooking.staff.entity.Staff;
import com.example.spabooking.staff.repository.StaffRepository;
import com.example.spabooking.tenant.context.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StaffService {

    private final StaffRepository staffRepository;

    @Autowired
    public StaffService(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
    }

    public List<Staff> findAll() {
        Long tenantId = TenantContext.requireTenantId();
        return staffRepository.findAllByTenantId(tenantId);
    }

    public Optional<Staff> findById(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        return staffRepository.findByIdAndTenantId(id, tenantId);
    }
}
