package com.example.spabooking.staff.service;

import com.example.spabooking.auth.entity.User;
import com.example.spabooking.auth.enums.UserRole;
import com.example.spabooking.auth.repository.UserRepository;
import com.example.spabooking.common.exception.ResourceNotFoundException;
import com.example.spabooking.staff.dto.CreateStaffAccountRequest;
import com.example.spabooking.staff.dto.StaffAccountResponse;
import com.example.spabooking.staff.dto.StaffResponse;
import com.example.spabooking.staff.entity.Staff;
import com.example.spabooking.staff.repository.StaffRepository;
import com.example.spabooking.tenant.context.TenantContext;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StaffService {

    private final StaffRepository staffRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public StaffService(StaffRepository staffRepository,
                        TenantRepository tenantRepository,
                        UserRepository userRepository,
                        PasswordEncoder passwordEncoder) {
        this.staffRepository = staffRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<StaffResponse> findAllWithAccounts() {
        Long tenantId = TenantContext.requireTenantId();
        List<Staff> staffList = staffRepository.findAllByTenantIdAndIsActiveTrue(tenantId);
        return staffList.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<Staff> findAll() {
        Long tenantId = TenantContext.requireTenantId();
        return staffRepository.findAllByTenantIdAndIsActiveTrue(tenantId);
    }

    public Optional<Staff> findById(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        return staffRepository.findByIdAndTenantIdAndIsActiveTrue(id, tenantId);
    }

    public StaffResponse findResponseById(Long id) {
        Staff staff = findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));
        return toResponse(staff);
    }

    @Transactional
    public Staff create(Staff staff) {
        Long tenantId = TenantContext.requireTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        staff.setTenant(tenant);
        return staffRepository.save(staff);
    }

    @Transactional
    public Staff update(Long id, Staff updatedDetails) {
        Long tenantId = TenantContext.requireTenantId();
        Staff existingStaff = staffRepository.findByIdAndTenantIdAndIsActiveTrue(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        existingStaff.setName(updatedDetails.getName());
        existingStaff.setPhone(updatedDetails.getPhone());
        existingStaff.setEmail(updatedDetails.getEmail());
        if (updatedDetails.getAvatarUrl() != null) {
            existingStaff.setAvatarUrl(updatedDetails.getAvatarUrl());
        }

        if (updatedDetails.getIsActive() != null) {
            existingStaff.setIsActive(updatedDetails.getIsActive());
            // Sync status with associated user account if present
            userRepository.findByStaffIdAndTenantId(id, tenantId).ifPresent(user -> {
                user.setIsActive(updatedDetails.getIsActive());
                userRepository.save(user);
            });
        }

        return staffRepository.save(existingStaff);
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        Staff existingStaff = staffRepository.findByIdAndTenantIdAndIsActiveTrue(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        existingStaff.setIsActive(false);
        staffRepository.save(existingStaff);

        // Deactivate associated user account
        userRepository.findByStaffIdAndTenantId(id, tenantId).ifPresent(user -> {
            user.setIsActive(false);
            userRepository.save(user);
        });
    }

    @Transactional
    public StaffAccountResponse createStaffAccount(Long staffId, CreateStaffAccountRequest request) {
        Long tenantId = TenantContext.requireTenantId();

        Staff staff = staffRepository.findByIdAndTenantIdAndIsActiveTrue(staffId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found or inactive"));

        if (userRepository.findByStaffId(staffId).isPresent()) {
            throw new IllegalArgumentException("Staff already has an associated user account");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.STAFF);
        user.setTenant(staff.getTenant());
        user.setStaff(staff);
        user.setIsActive(true);

        User savedUser = userRepository.save(user);
        return StaffAccountResponse.fromEntity(savedUser);
    }

    public StaffResponse toResponse(Staff staff) {
        String username = userRepository.findByStaffId(staff.getId())
                .map(User::getUsername)
                .orElse(null);
        return StaffResponse.fromEntity(staff, username);
    }
}
