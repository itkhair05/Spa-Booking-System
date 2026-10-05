package com.example.spabooking.service.controller;

import com.example.spabooking.service.dto.CreateServiceCategoryRequest;
import com.example.spabooking.service.dto.ServiceCategoryResponse;
import com.example.spabooking.service.dto.UpdateServiceCategoryRequest;
import com.example.spabooking.service.service.ServiceCategoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/service-categories")
public class ServiceCategoryController {

    private final ServiceCategoryService serviceCategoryService;

    @Autowired
    public ServiceCategoryController(ServiceCategoryService serviceCategoryService) {
        this.serviceCategoryService = serviceCategoryService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<List<ServiceCategoryResponse>> getAll() {
        return ResponseEntity.ok(serviceCategoryService.getAllCategories());
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<List<ServiceCategoryResponse>> getActive() {
        return ResponseEntity.ok(serviceCategoryService.getActiveCategories());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<ServiceCategoryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(serviceCategoryService.getCategoryById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ServiceCategoryResponse> create(@Valid @RequestBody CreateServiceCategoryRequest request) {
        ServiceCategoryResponse created = serviceCategoryService.createCategory(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ServiceCategoryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateServiceCategoryRequest request) {
        ServiceCategoryResponse updated = serviceCategoryService.updateCategory(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        serviceCategoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
