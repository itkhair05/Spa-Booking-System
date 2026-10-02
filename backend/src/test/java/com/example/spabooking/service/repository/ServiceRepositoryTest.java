package com.example.spabooking.service.repository;

import com.example.spabooking.service.entity.Service;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ServiceRepositoryTest {

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private TenantRepository tenantRepository;

    private Tenant tenantA;
    private Tenant tenantB;

    private Service serviceA;
    private Service serviceB;

    @BeforeEach
    void setUp() {
        tenantA = new Tenant();
        tenantA.setName("Tenant A");
        tenantA.setSlug("tenant-a");
        tenantA = tenantRepository.save(tenantA);

        tenantB = new Tenant();
        tenantB.setName("Tenant B");
        tenantB.setSlug("tenant-b");
        tenantB = tenantRepository.save(tenantB);

        serviceA = new Service();
        serviceA.setTenant(tenantA);
        serviceA.setName("Service A");
        serviceA.setDurationMinutes(60);
        serviceA.setPrice(new BigDecimal("100.00"));
        serviceA = serviceRepository.save(serviceA);

        serviceB = new Service();
        serviceB.setTenant(tenantB);
        serviceB.setName("Service B");
        serviceB.setDurationMinutes(30);
        serviceB.setPrice(new BigDecimal("50.00"));
        serviceB = serviceRepository.save(serviceB);
    }

    @Test
    void testFindAllByTenantId() {
        List<Service> servicesA = serviceRepository.findAllByTenantId(tenantA.getId());
        assertEquals(1, servicesA.size());
        assertEquals("Service A", servicesA.get(0).getName());

        List<Service> servicesB = serviceRepository.findAllByTenantId(tenantB.getId());
        assertEquals(1, servicesB.size());
        assertEquals("Service B", servicesB.get(0).getName());
    }

    @Test
    void testFindByIdAndTenantId() {
        Optional<Service> foundA = serviceRepository.findByIdAndTenantId(serviceA.getId(), tenantA.getId());
        assertTrue(foundA.isPresent());
        assertEquals("Service A", foundA.get().getName());

        // Tenant A trying to find Tenant B's service
        Optional<Service> crossTenantFind = serviceRepository.findByIdAndTenantId(serviceB.getId(), tenantA.getId());
        assertFalse(crossTenantFind.isPresent());
    }
}
