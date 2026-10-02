package com.example.spabooking.staff.repository;

import com.example.spabooking.staff.entity.Staff;
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
public class StaffRepositoryTest {

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private TenantRepository tenantRepository;

    private Tenant tenantA;
    private Tenant tenantB;

    private Staff staffA;
    private Staff staffB;

    @BeforeEach
    void setUp() {
        tenantA = new Tenant();
        tenantA.setName("Tenant A");
        tenantA.setSlug("tenant-a-staff");
        tenantA = tenantRepository.save(tenantA);

        tenantB = new Tenant();
        tenantB.setName("Tenant B");
        tenantB.setSlug("tenant-b-staff");
        tenantB = tenantRepository.save(tenantB);

        staffA = new Staff();
        staffA.setTenant(tenantA);
        staffA.setName("Staff A");
        staffA = staffRepository.save(staffA);

        staffB = new Staff();
        staffB.setTenant(tenantB);
        staffB.setName("Staff B");
        staffB = staffRepository.save(staffB);
    }

    @Test
    void testFindAllByTenantId() {
        List<Staff> staffListA = staffRepository.findAllByTenantId(tenantA.getId());
        assertEquals(1, staffListA.size());
        assertEquals("Staff A", staffListA.get(0).getName());

        List<Staff> staffListB = staffRepository.findAllByTenantId(tenantB.getId());
        assertEquals(1, staffListB.size());
        assertEquals("Staff B", staffListB.get(0).getName());
    }

    @Test
    void testFindByIdAndTenantId() {
        Optional<Staff> foundA = staffRepository.findByIdAndTenantId(staffA.getId(), tenantA.getId());
        assertTrue(foundA.isPresent());
        assertEquals("Staff A", foundA.get().getName());

        // Tenant A trying to find Tenant B's staff
        Optional<Staff> crossTenantFind = staffRepository.findByIdAndTenantId(staffB.getId(), tenantA.getId());
        assertFalse(crossTenantFind.isPresent());
    }
}
