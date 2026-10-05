package com.example.spabooking.service.service;

import com.example.spabooking.common.exception.ResourceNotFoundException;
import com.example.spabooking.service.dto.CreateServiceCategoryRequest;
import com.example.spabooking.service.dto.ServiceCategoryResponse;
import com.example.spabooking.service.dto.UpdateServiceCategoryRequest;
import com.example.spabooking.service.entity.ServiceCategory;
import com.example.spabooking.service.repository.ServiceCategoryRepository;
import com.example.spabooking.service.repository.ServiceRepository;
import com.example.spabooking.tenant.context.TenantContext;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ServiceCategoryService {

    private final ServiceCategoryRepository serviceCategoryRepository;
    private final ServiceRepository serviceRepository;
    private final TenantRepository tenantRepository;

    @Autowired
    public ServiceCategoryService(ServiceCategoryRepository serviceCategoryRepository,
                                  ServiceRepository serviceRepository,
                                  TenantRepository tenantRepository) {
        this.serviceCategoryRepository = serviceCategoryRepository;
        this.serviceRepository = serviceRepository;
        this.tenantRepository = tenantRepository;
    }

    public List<ServiceCategoryResponse> getAllCategories() {
        Long tenantId = TenantContext.requireTenantId();
        return serviceCategoryRepository.findAllByTenantIdOrderByDisplayOrderAsc(tenantId)
                .stream()
                .map(ServiceCategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ServiceCategoryResponse> getActiveCategories() {
        Long tenantId = TenantContext.requireTenantId();
        return serviceCategoryRepository.findAllByTenantIdAndIsActiveTrueOrderByDisplayOrderAsc(tenantId)
                .stream()
                .map(ServiceCategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public ServiceCategoryResponse getCategoryById(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ServiceCategory category = serviceCategoryRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục không tồn tại"));
        return ServiceCategoryResponse.fromEntity(category);
    }

    @Transactional
    public ServiceCategoryResponse createCategory(CreateServiceCategoryRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        if (serviceCategoryRepository.existsByTenantIdAndNameIgnoreCase(tenantId, request.getName().trim())) {
            throw new IllegalArgumentException("Tên danh mục đã tồn tại");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        ServiceCategory category = new ServiceCategory();
        category.setTenant(tenant);
        category.setName(request.getName().trim());
        category.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        category.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        category.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        ServiceCategory saved = serviceCategoryRepository.save(category);
        return ServiceCategoryResponse.fromEntity(saved);
    }

    @Transactional
    public ServiceCategoryResponse updateCategory(Long id, UpdateServiceCategoryRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ServiceCategory category = serviceCategoryRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục không tồn tại"));

        if (request.getName() != null && !request.getName().isBlank()) {
            String newName = request.getName().trim();
            if (!newName.equalsIgnoreCase(category.getName()) &&
                    serviceCategoryRepository.existsByTenantIdAndNameIgnoreCase(tenantId, newName)) {
                throw new IllegalArgumentException("Tên danh mục đã tồn tại");
            }
            category.setName(newName);
        }

        if (request.getDescription() != null) {
            category.setDescription(request.getDescription().trim());
        }

        if (request.getDisplayOrder() != null) {
            category.setDisplayOrder(request.getDisplayOrder());
        }

        if (request.getIsActive() != null) {
            category.setIsActive(request.getIsActive());
        }

        ServiceCategory saved = serviceCategoryRepository.save(category);
        return ServiceCategoryResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ServiceCategory category = serviceCategoryRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục không tồn tại"));

        // Do not allow deleting category that still has services
        if (serviceRepository.existsByTenantIdAndCategoryId(tenantId, id)) {
            throw new IllegalStateException("Không thể xóa danh mục đang có dịch vụ liên kết. Vui lòng chuyển các dịch vụ sang danh mục khác trước.");
        }

        serviceCategoryRepository.delete(category);
    }
}
