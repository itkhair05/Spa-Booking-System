package com.example.spabooking.customer.service;

import com.example.spabooking.customer.entity.Customer;
import com.example.spabooking.customer.repository.CustomerRepository;
import com.example.spabooking.tenant.context.TenantContext;
import com.example.spabooking.common.exception.ResourceNotFoundException;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final TenantRepository tenantRepository;

    @Autowired
    public CustomerService(CustomerRepository customerRepository, TenantRepository tenantRepository) {
        this.customerRepository = customerRepository;
        this.tenantRepository = tenantRepository;
    }

    public List<Customer> findAll() {
        Long tenantId = TenantContext.requireTenantId();
        return customerRepository.findAllByTenantIdAndIsActiveTrue(tenantId);
    }

    public Optional<Customer> findById(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        return customerRepository.findByIdAndTenantIdAndIsActiveTrue(id, tenantId);
    }

    public Customer create(Customer customer) {
        Long tenantId = TenantContext.requireTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        customer.setTenant(tenant);
        return customerRepository.save(customer);
    }

    public Customer update(Long id, Customer updatedDetails) {
        Long tenantId = TenantContext.requireTenantId();
        Customer existingCustomer = customerRepository.findByIdAndTenantIdAndIsActiveTrue(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        existingCustomer.setName(updatedDetails.getName());
        existingCustomer.setPhone(updatedDetails.getPhone());
        existingCustomer.setEmail(updatedDetails.getEmail());
        // lastVisit is untouched here.

        return customerRepository.save(existingCustomer);
    }

    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        Customer existingCustomer = customerRepository.findByIdAndTenantIdAndIsActiveTrue(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        existingCustomer.setIsActive(false);
        customerRepository.save(existingCustomer);
    }
}
