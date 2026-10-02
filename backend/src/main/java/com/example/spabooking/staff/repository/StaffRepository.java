package com.example.spabooking.staff.repository;

import com.example.spabooking.staff.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {
    
    List<Staff> findAllByTenantId(Long tenantId);
    
    Optional<Staff> findByIdAndTenantId(Long id, Long tenantId);
}
