package com.example.spabooking.service.service;

import com.example.spabooking.service.entity.Service;
import com.example.spabooking.service.repository.ServiceRepository;
import com.example.spabooking.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ServiceServiceTest {

    @Mock
    private ServiceRepository serviceRepository;

    @InjectMocks
    private ServiceService serviceService;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() throws Exception {
        TenantContext.clear();
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    void testFindAllWithTenantContext() {
        TenantContext.setTenantId(10L);
        Service mockService = new Service();
        mockService.setId(1L);
        when(serviceRepository.findAllByTenantId(10L)).thenReturn(Collections.singletonList(mockService));

        List<Service> result = serviceService.findAll();
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        verify(serviceRepository, times(1)).findAllByTenantId(10L);
    }

    @Test
    void testFindByIdWithTenantContext() {
        TenantContext.setTenantId(20L);
        Service mockService = new Service();
        mockService.setId(2L);
        when(serviceRepository.findByIdAndTenantId(2L, 20L)).thenReturn(Optional.of(mockService));

        Optional<Service> result = serviceService.findById(2L);
        assertTrue(result.isPresent());
        assertEquals(2L, result.get().getId());
        verify(serviceRepository, times(1)).findByIdAndTenantId(2L, 20L);
    }

    @Test
    void testFindByIdCrossTenantDenied() {
        TenantContext.setTenantId(30L);
        when(serviceRepository.findByIdAndTenantId(3L, 30L)).thenReturn(Optional.empty());

        Optional<Service> result = serviceService.findById(3L);
        assertFalse(result.isPresent(), "Cross-tenant access should return empty Optional (Not Found)");
        verify(serviceRepository, times(1)).findByIdAndTenantId(3L, 30L);
    }

    @Test
    void testWithoutTenantContextThrowsException() {
        // TenantContext is empty
        assertThrows(IllegalStateException.class, () -> serviceService.findAll(), "Expected requireTenantId to throw exception");
        assertThrows(IllegalStateException.class, () -> serviceService.findById(1L), "Expected requireTenantId to throw exception");
        
        verify(serviceRepository, never()).findAllByTenantId(any());
        verify(serviceRepository, never()).findByIdAndTenantId(any(), any());
    }
}
