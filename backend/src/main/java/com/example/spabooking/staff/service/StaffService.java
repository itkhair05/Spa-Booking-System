package com.example.spabooking.staff.service;

import com.example.spabooking.auth.entity.User;
import com.example.spabooking.auth.enums.UserRole;
import com.example.spabooking.auth.repository.UserRepository;
import com.example.spabooking.common.exception.ResourceNotFoundException;
import com.example.spabooking.auth.security.CustomUserDetails;
import com.example.spabooking.auth.security.JwtUtils;
import com.example.spabooking.booking.repository.BookingRepository;
import com.example.spabooking.staff.dto.CreateStaffAccountRequest;
import com.example.spabooking.staff.dto.StaffAccountResponse;
import com.example.spabooking.staff.dto.StaffResponse;
import com.example.spabooking.staff.dto.UpdateStaffSelfProfileRequest;
import com.example.spabooking.staff.entity.Staff;
import com.example.spabooking.staff.repository.StaffRepository;
import com.example.spabooking.tenant.context.TenantContext;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StaffService {

    private final StaffRepository staffRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final BookingRepository bookingRepository;
    private final StaffScheduleService staffScheduleService;

    @Autowired
    public StaffService(StaffRepository staffRepository,
                        TenantRepository tenantRepository,
                        UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        JwtUtils jwtUtils,
                        BookingRepository bookingRepository,
                        StaffScheduleService staffScheduleService) {
        this.staffRepository = staffRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.bookingRepository = bookingRepository;
        this.staffScheduleService = staffScheduleService;
    }

    public List<StaffResponse> findAllWithAccounts() {
        Long tenantId = TenantContext.requireTenantId();
        // Return non-deleted staff for tenant so OWNER can see active & inactive staff, but not deleted
        List<Staff> staffList = staffRepository.findAllByTenantIdAndIsDeletedFalse(tenantId);
        if (staffList.isEmpty()) {
            return List.of();
        }
        Map<Long, User> usersByStaffId = userRepository
                .findAllByStaffIdIn(staffList.stream().map(Staff::getId).collect(Collectors.toList()))
                .stream()
                .filter(user -> user.getStaff() != null)
                .collect(Collectors.toMap(user -> user.getStaff().getId(), Function.identity(), (a, b) -> a));
        return staffList.stream()
                .map(staff -> toResponse(staff, usersByStaffId.get(staff.getId())))
                .collect(Collectors.toList());
    }

    public List<Staff> findAll() {
        Long tenantId = TenantContext.requireTenantId();
        return staffRepository.findAllByTenantIdAndIsActiveTrueAndIsDeletedFalse(tenantId);
    }

    public Optional<Staff> findById(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        return staffRepository.findByIdAndTenantIdAndIsActiveTrueAndIsDeletedFalse(id, tenantId);
    }

    public StaffResponse findResponseById(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        Staff staff = staffRepository.findByIdAndTenantIdAndIsDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));
        return toResponse(staff);
    }

    @Transactional
    public Staff create(Staff staff) {
        Long tenantId = TenantContext.requireTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        staff.setTenant(tenant);
        staff.setIsDeleted(false);
        Staff saved = staffRepository.save(staff);
        staffScheduleService.initDefaultWorkingHours(saved);
        return saved;
    }

    @Transactional
    public Staff update(Long id, Staff updatedDetails) {
        Long tenantId = TenantContext.requireTenantId();
        Staff existingStaff = staffRepository.findByIdAndTenantIdAndIsDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        if (updatedDetails.getName() != null) {
            existingStaff.setName(updatedDetails.getName());
        }
        if (updatedDetails.getPhone() != null) {
            existingStaff.setPhone(updatedDetails.getPhone());
        }
        if (updatedDetails.getEmail() != null) {
            existingStaff.setEmail(updatedDetails.getEmail());
        }
        if (updatedDetails.getAvatarUrl() != null) {
            existingStaff.setAvatarUrl(updatedDetails.getAvatarUrl());
        }

        if (updatedDetails.getShowOnWebsite() != null) {
            existingStaff.setShowOnWebsite(updatedDetails.getShowOnWebsite());
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
        Staff existingStaff = staffRepository.findByIdAndTenantIdAndIsDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        // Archive / soft-delete staff: preserve historical bookings and foreign key integrity
        existingStaff.setIsDeleted(true);
        existingStaff.setIsActive(false);
        staffRepository.save(existingStaff);

        // Deactivate associated user account
        userRepository.findByStaffIdAndTenantId(id, tenantId).ifPresent(user -> {
            user.setIsActive(false);
            userRepository.save(user);
        });
    }

    @Transactional
    public StaffResponse updateMyProfile(Long staffId, UpdateStaffSelfProfileRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        Staff staff = staffRepository.findByIdAndTenantIdAndIsDeletedFalse(staffId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        if (request.getName() != null && !request.getName().isBlank()) {
            staff.setName(request.getName().trim());
        }
        if (request.getPhone() != null) {
            staff.setPhone(request.getPhone().trim());
        }
        if (request.getAvatarUrl() != null) {
            staff.setAvatarUrl(request.getAvatarUrl());
        }

        String newAccessToken = null;
        Optional<User> linkedUserOpt = userRepository.findByStaffIdAndTenantId(staffId, tenantId);

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String newEmail = request.getEmail().trim().toLowerCase();
            staff.setEmail(newEmail);

            if (linkedUserOpt.isPresent()) {
                User user = linkedUserOpt.get();
                if (!newEmail.equalsIgnoreCase(user.getUsername())) {
                    // Check if new email is already taken by another user
                    Optional<User> existingUser = userRepository.findByUsername(newEmail);
                    if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
                        throw new IllegalArgumentException("Email / Tên đăng nhập này đã được sử dụng");
                    }
                    user.setUsername(newEmail);
                    userRepository.save(user);

                    // Generate fresh token with the new username so session remains valid
                    CustomUserDetails userDetails = new CustomUserDetails(user);
                    Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    newAccessToken = jwtUtils.generateJwtToken(auth);
                }
            }
        }

        Staff savedStaff = staffRepository.save(staff);
        User user = linkedUserOpt.orElse(null);
        StaffResponse response = toResponse(savedStaff, user);
        if (newAccessToken != null) {
            response.setAccessToken(newAccessToken);
        }
        return response;
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
        User user = userRepository.findByStaffId(staff.getId()).orElse(null);
        return toResponse(staff, user);
    }

    private StaffResponse toResponse(Staff staff, User user) {
        String username = user != null ? user.getUsername() : null;
        Boolean accountEnabled = user != null ? user.getIsActive() : null;
        return StaffResponse.fromEntity(staff, username, accountEnabled);
    }
}
