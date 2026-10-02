package com.example.spabooking.staff.repository;

import com.example.spabooking.staff.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {
    List<Staff> findAllByTenantIdAndIsActiveTrue(Long tenantId);
    
    Optional<Staff> findByIdAndTenantIdAndIsActiveTrue(Long id, Long tenantId);

    List<Staff> findAllByTenantId(Long tenantId);
    
    Optional<Staff> findByIdAndTenantId(Long id, Long tenantId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT s FROM Staff s WHERE s.id = :id AND s.tenant.id = :tenantId AND s.isActive = true")
    Optional<Staff> findByIdAndTenantIdAndIsActiveTrueForUpdate(@org.springframework.data.repository.query.Param("id") Long id, @org.springframework.data.repository.query.Param("tenantId") Long tenantId);
}
