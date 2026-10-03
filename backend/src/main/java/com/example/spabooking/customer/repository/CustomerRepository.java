package com.example.spabooking.customer.repository;

import com.example.spabooking.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    
    List<Customer> findAllByTenantId(Long tenantId);
    
    Optional<Customer> findByIdAndTenantId(Long id, Long tenantId);

    List<Customer> findAllByTenantIdAndIsActiveTrue(Long tenantId);

    Optional<Customer> findByIdAndTenantIdAndIsActiveTrue(Long id, Long tenantId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT c FROM Customer c WHERE c.id = :id AND c.tenant.id = :tenantId AND c.isActive = true")
    Optional<Customer> findByIdAndTenantIdAndIsActiveTrueForUpdate(@org.springframework.data.repository.query.Param("id") Long id, @org.springframework.data.repository.query.Param("tenantId") Long tenantId);

    Optional<Customer> findByPhoneAndTenantId(String phone, Long tenantId);
}
