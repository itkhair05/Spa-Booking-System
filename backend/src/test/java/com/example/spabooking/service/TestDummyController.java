package com.example.spabooking.service;

import com.example.spabooking.customer.service.CustomerService;
import com.example.spabooking.service.service.ServiceService;
import com.example.spabooking.staff.service.StaffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test-isolation")
public class TestDummyController {

    private final ServiceService serviceService;
    private final StaffService staffService;
    private final CustomerService customerService;

    @Autowired
    public TestDummyController(ServiceService serviceService, StaffService staffService, CustomerService customerService) {
        this.serviceService = serviceService;
        this.staffService = staffService;
        this.customerService = customerService;
    }

    @GetMapping("/services/{id}")
    public ResponseEntity<?> getService(@PathVariable Long id) {
        return serviceService.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/staff/{id}")
    public ResponseEntity<?> getStaff(@PathVariable Long id) {
        return staffService.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<?> getCustomer(@PathVariable Long id) {
        return customerService.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
