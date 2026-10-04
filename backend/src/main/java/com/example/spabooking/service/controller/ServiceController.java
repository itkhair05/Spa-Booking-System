package com.example.spabooking.service.controller;

import com.example.spabooking.common.exception.ResourceNotFoundException;
import com.example.spabooking.service.dto.CreateServiceRequest;
import com.example.spabooking.service.dto.ServiceResponse;
import com.example.spabooking.service.dto.UpdateServiceRequest;
import com.example.spabooking.service.entity.Service;
import com.example.spabooking.service.service.ServiceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;
import com.example.spabooking.common.storage.FileStorageService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/services")
public class ServiceController {

    private final ServiceService serviceService;
    private final FileStorageService fileStorageService;

    @Autowired
    public ServiceController(ServiceService serviceService, FileStorageService fileStorageService) {
        this.serviceService = serviceService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<List<ServiceResponse>> getAllServices() {
        List<Service> services = serviceService.findAll();
        List<ServiceResponse> response = services.stream()
                .map(ServiceResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<ServiceResponse> getServiceById(@PathVariable Long id) {
        Service service = serviceService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        return ResponseEntity.ok(ServiceResponse.fromEntity(service));
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ServiceResponse> createService(@Valid @RequestBody CreateServiceRequest request) {
        Service service = new Service();
        service.setName(request.getName());
        service.setDescription(request.getDescription());
        service.setDurationMinutes(request.getDurationMinutes());
        service.setPrice(request.getPrice());
        service.setImageUrl(request.getImageUrl());
        if (request.getIsActive() != null) {
            service.setIsActive(request.getIsActive());
        }

        Service createdService = serviceService.create(service);
        return new ResponseEntity<>(ServiceResponse.fromEntity(createdService), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ServiceResponse> updateService(
            @PathVariable Long id,
            @Valid @RequestBody UpdateServiceRequest request) {
        Service serviceDetails = new Service();
        serviceDetails.setName(request.getName());
        serviceDetails.setDescription(request.getDescription());
        serviceDetails.setDurationMinutes(request.getDurationMinutes());
        serviceDetails.setPrice(request.getPrice());
        serviceDetails.setImageUrl(request.getImageUrl());
        if (request.getIsActive() != null) {
            serviceDetails.setIsActive(request.getIsActive());
        }

        Service updatedService = serviceService.update(id, serviceDetails);
        return ResponseEntity.ok(ServiceResponse.fromEntity(updatedService));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        serviceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/image")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ServiceResponse> uploadServiceImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        String fileUrl = fileStorageService.storeFile(file, "services");
        Service service = serviceService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        service.setImageUrl(fileUrl);
        Service updated = serviceService.update(id, service);
        return ResponseEntity.ok(ServiceResponse.fromEntity(updated));
    }

    @DeleteMapping("/{id}/image")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> deleteServiceImage(@PathVariable Long id) {
        Service service = serviceService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        if (service.getImageUrl() != null) {
            fileStorageService.deleteFileByUrl(service.getImageUrl());
            service.setImageUrl(null);
            serviceService.update(id, service);
        }
        return ResponseEntity.noContent().build();
    }
}
