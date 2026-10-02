package com.example.spabooking.customer.service;

import com.example.spabooking.customer.entity.Customer;
import com.example.spabooking.customer.repository.CustomerRepository;
import com.example.spabooking.tenant.context.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Autowired
    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<Customer> findAll() {
        Long tenantId = TenantContext.requireTenantId();
        return customerRepository.findAllByTenantId(tenantId);
    }

    public Optional<Customer> findById(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        return customerRepository.findByIdAndTenantId(id, tenantId);
    }
}
