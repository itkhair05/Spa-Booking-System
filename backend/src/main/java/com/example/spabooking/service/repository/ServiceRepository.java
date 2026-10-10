package com.example.spabooking.service.repository;

import com.example.spabooking.service.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {
    
    @Query("SELECT s FROM Service s LEFT JOIN FETCH s.category WHERE s.tenant.id = :tenantId")
    List<Service> findAllByTenantId(@Param("tenantId") Long tenantId);
    
    @Query("SELECT s FROM Service s LEFT JOIN FETCH s.category WHERE s.id = :id AND s.tenant.id = :tenantId")
    Optional<Service> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT s FROM Service s LEFT JOIN FETCH s.category WHERE s.tenant.id = :tenantId AND s.isActive = true")
    List<Service> findAllByTenantIdAndIsActiveTrue(@Param("tenantId") Long tenantId);

    @Query("SELECT s FROM Service s LEFT JOIN FETCH s.category WHERE s.tenant.id = :tenantId AND s.isActive = true AND s.isFeatured = true")
    List<Service> findAllByTenantIdAndIsActiveTrueAndIsFeaturedTrue(@Param("tenantId") Long tenantId);

    @Query("SELECT s FROM Service s LEFT JOIN FETCH s.category WHERE s.tenant.id = :tenantId AND s.category.id = :categoryId")
    List<Service> findAllByTenantIdAndCategoryId(@Param("tenantId") Long tenantId, @Param("categoryId") Long categoryId);

    boolean existsByTenantIdAndCategoryId(Long tenantId, Long categoryId);
}
