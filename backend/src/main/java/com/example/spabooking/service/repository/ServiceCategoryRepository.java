package com.example.spabooking.service.repository;

import com.example.spabooking.service.entity.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, Long> {

    List<ServiceCategory> findAllByTenantId(Long tenantId);

    List<ServiceCategory> findAllByTenantIdOrderByDisplayOrderAsc(Long tenantId);

    List<ServiceCategory> findAllByTenantIdAndIsActiveTrueOrderByDisplayOrderAsc(Long tenantId);

    Optional<ServiceCategory> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByTenantIdAndNameIgnoreCase(Long tenantId, String name);
}
