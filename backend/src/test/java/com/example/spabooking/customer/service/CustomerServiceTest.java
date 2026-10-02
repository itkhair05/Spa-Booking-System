package com.example.spabooking.customer.service;

import com.example.spabooking.customer.entity.Customer;
import com.example.spabooking.customer.repository.CustomerRepository;
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

public class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

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
        Customer mockCustomer = new Customer();
        mockCustomer.setId(1L);
        when(customerRepository.findAllByTenantIdAndIsActiveTrue(10L)).thenReturn(Collections.singletonList(mockCustomer));

        List<Customer> result = customerService.findAll();
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        verify(customerRepository, times(1)).findAllByTenantIdAndIsActiveTrue(10L);
    }

    @Test
    void testFindByIdWithTenantContext() {
        TenantContext.setTenantId(20L);
        Customer mockCustomer = new Customer();
        mockCustomer.setId(2L);
        when(customerRepository.findByIdAndTenantIdAndIsActiveTrue(2L, 20L)).thenReturn(Optional.of(mockCustomer));

        Optional<Customer> result = customerService.findById(2L);
        assertTrue(result.isPresent());
        assertEquals(2L, result.get().getId());
        verify(customerRepository, times(1)).findByIdAndTenantIdAndIsActiveTrue(2L, 20L);
    }

    @Test
    void testFindByIdCrossTenantDenied() {
        TenantContext.setTenantId(30L);
        when(customerRepository.findByIdAndTenantIdAndIsActiveTrue(3L, 30L)).thenReturn(Optional.empty());

        Optional<Customer> result = customerService.findById(3L);
        assertFalse(result.isPresent(), "Cross-tenant access should return empty Optional (Not Found)");
        verify(customerRepository, times(1)).findByIdAndTenantIdAndIsActiveTrue(3L, 30L);
    }

    @Test
    void testWithoutTenantContextThrowsException() {
        assertThrows(IllegalStateException.class, () -> customerService.findAll(), "Expected requireTenantId to throw exception");
        assertThrows(IllegalStateException.class, () -> customerService.findById(1L), "Expected requireTenantId to throw exception");
        
        verify(customerRepository, never()).findAllByTenantIdAndIsActiveTrue(any());
        verify(customerRepository, never()).findByIdAndTenantIdAndIsActiveTrue(any(), any());
    }
}
