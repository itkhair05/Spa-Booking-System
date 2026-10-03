package com.example.spabooking.service.repository;

import com.example.spabooking.service.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {
    
    List<Service> findAllByTenantId(Long tenantId);
    
    Optional<Service> findByIdAndTenantId(Long id, Long tenantId);

    List<Service> findAllByTenantIdAndIsActiveTrue(Long tenantId);
}
