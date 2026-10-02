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
}
