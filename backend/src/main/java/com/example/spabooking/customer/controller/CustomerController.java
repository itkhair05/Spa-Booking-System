package com.example.spabooking.customer.controller;

import com.example.spabooking.common.exception.ResourceNotFoundException;
import com.example.spabooking.customer.dto.CreateCustomerRequest;
import com.example.spabooking.customer.dto.CustomerResponse;
import com.example.spabooking.customer.dto.UpdateCustomerRequest;
import com.example.spabooking.customer.entity.Customer;
import com.example.spabooking.customer.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    @Autowired
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        List<Customer> customers = customerService.findAll();
        List<CustomerResponse> response = customers.stream()
                .map(CustomerResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable Long id) {
        Customer customer = customerService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        return ResponseEntity.ok(CustomerResponse.fromEntity(customer));
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        Customer customer = new Customer();
        customer.setName(request.getName());
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());

        Customer createdCustomer = customerService.create(customer);
        return new ResponseEntity<>(CustomerResponse.fromEntity(createdCustomer), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCustomerRequest request) {
        Customer details = new Customer();
        details.setName(request.getName());
        details.setPhone(request.getPhone());
        details.setEmail(request.getEmail());

        Customer updatedCustomer = customerService.update(id, details);
        return ResponseEntity.ok(CustomerResponse.fromEntity(updatedCustomer));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
