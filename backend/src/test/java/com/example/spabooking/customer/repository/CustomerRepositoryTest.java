package com.example.spabooking.customer.repository;

import com.example.spabooking.customer.entity.Customer;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TenantRepository tenantRepository;

    private Tenant tenantA;
    private Tenant tenantB;

    private Customer customerA;
    private Customer customerB;

    @BeforeEach
    void setUp() {
        tenantA = new Tenant();
        tenantA.setName("Tenant A");
        tenantA.setSlug("tenant-a-customer");
        tenantA = tenantRepository.save(tenantA);

        tenantB = new Tenant();
        tenantB.setName("Tenant B");
        tenantB.setSlug("tenant-b-customer");
        tenantB = tenantRepository.save(tenantB);

        customerA = new Customer();
        customerA.setTenant(tenantA);
        customerA.setName("Customer A");
        customerA = customerRepository.save(customerA);

        customerB = new Customer();
        customerB.setTenant(tenantB);
        customerB.setName("Customer B");
        customerB = customerRepository.save(customerB);
    }

    @Test
    void testFindAllByTenantId() {
        List<Customer> customerListA = customerRepository.findAllByTenantId(tenantA.getId());
        assertEquals(1, customerListA.size());
        assertEquals("Customer A", customerListA.get(0).getName());

        List<Customer> customerListB = customerRepository.findAllByTenantId(tenantB.getId());
        assertEquals(1, customerListB.size());
        assertEquals("Customer B", customerListB.get(0).getName());
    }

    @Test
    void testFindByIdAndTenantId() {
        Optional<Customer> foundA = customerRepository.findByIdAndTenantId(customerA.getId(), tenantA.getId());
        assertTrue(foundA.isPresent());
        assertEquals("Customer A", foundA.get().getName());

        // Tenant A trying to find Tenant B's customer
        Optional<Customer> crossTenantFind = customerRepository.findByIdAndTenantId(customerB.getId(), tenantA.getId());
        assertFalse(crossTenantFind.isPresent());
    }
}
