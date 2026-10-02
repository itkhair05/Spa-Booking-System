package com.example.spabooking.staff.service;

import com.example.spabooking.staff.entity.Staff;
import com.example.spabooking.staff.repository.StaffRepository;
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

public class StaffServiceTest {

    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private StaffService staffService;

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
        Staff mockStaff = new Staff();
        mockStaff.setId(1L);
        when(staffRepository.findAllByTenantIdAndIsActiveTrue(10L)).thenReturn(Collections.singletonList(mockStaff));

        List<Staff> result = staffService.findAll();
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        verify(staffRepository, times(1)).findAllByTenantIdAndIsActiveTrue(10L);
    }

    @Test
    void testFindByIdWithTenantContext() {
        TenantContext.setTenantId(20L);
        Staff mockStaff = new Staff();
        mockStaff.setId(2L);
        when(staffRepository.findByIdAndTenantIdAndIsActiveTrue(2L, 20L)).thenReturn(Optional.of(mockStaff));

        Optional<Staff> result = staffService.findById(2L);
        assertTrue(result.isPresent());
        assertEquals(2L, result.get().getId());
        verify(staffRepository, times(1)).findByIdAndTenantIdAndIsActiveTrue(2L, 20L);
    }

    @Test
    void testFindByIdCrossTenantDenied() {
        TenantContext.setTenantId(30L);
        when(staffRepository.findByIdAndTenantIdAndIsActiveTrue(3L, 30L)).thenReturn(Optional.empty());

        Optional<Staff> result = staffService.findById(3L);
        assertFalse(result.isPresent(), "Cross-tenant access should return empty Optional (Not Found)");
        verify(staffRepository, times(1)).findByIdAndTenantIdAndIsActiveTrue(3L, 30L);
    }

    @Test
    void testWithoutTenantContextThrowsException() {
        assertThrows(IllegalStateException.class, () -> staffService.findAll(), "Expected requireTenantId to throw exception");
        assertThrows(IllegalStateException.class, () -> staffService.findById(1L), "Expected requireTenantId to throw exception");
        
        verify(staffRepository, never()).findAllByTenantIdAndIsActiveTrue(any());
        verify(staffRepository, never()).findByIdAndTenantIdAndIsActiveTrue(any(), any());
    }
}
