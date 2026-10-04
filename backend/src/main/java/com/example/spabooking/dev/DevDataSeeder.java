package com.example.spabooking.dev;

import com.example.spabooking.auth.entity.User;
import com.example.spabooking.auth.enums.UserRole;
import com.example.spabooking.auth.repository.UserRepository;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import com.example.spabooking.service.entity.Service;
import com.example.spabooking.service.repository.ServiceRepository;
import com.example.spabooking.staff.entity.Staff;
import com.example.spabooking.staff.repository.StaffRepository;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

    static final String DEMO_TENANT_NAME = "TIKEY SPA";
    static final String DEMO_TENANT_SLUG = "tikey-spa";
    static final String LEGACY_TENANT_SLUG = "demo-spa";
    static final String DEMO_OWNER_USERNAME = "owner@demo.local";
    static final String DEMO_STAFF_USERNAME = "staff@demo.local";

    private static final Logger logger = LoggerFactory.getLogger(DevDataSeeder.class);

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ServiceRepository serviceRepository;
    private final StaffRepository staffRepository;
    private final String ownerPassword;
    private final String staffPassword;

    public DevDataSeeder(TenantRepository tenantRepository,
                         UserRepository userRepository,
                         PasswordEncoder passwordEncoder,
                         ServiceRepository serviceRepository,
                         StaffRepository staffRepository,
                         @Value("${dev.seed.owner-password}") String ownerPassword,
                         @Value("${dev.seed.staff-password}") String staffPassword) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.serviceRepository = serviceRepository;
        this.staffRepository = staffRepository;
        this.ownerPassword = ownerPassword;
        this.staffPassword = staffPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (ownerPassword == null || ownerPassword.isBlank()) {
            throw new IllegalStateException(
                    "DEV_OWNER_PASSWORD is not set. Demo data seeding requires a password and will not use a default.");
        }
        
        if (staffPassword == null || staffPassword.isBlank()) {
            throw new IllegalStateException(
                    "DEV_STAFF_PASSWORD is not set. Demo data seeding requires a password and will not use a default.");
        }

        Tenant tenant = tenantRepository.findBySlug(DEMO_TENANT_SLUG)
                .orElseGet(this::findLegacyOrCreateTenant);

        if (userRepository.findByUsername(DEMO_OWNER_USERNAME).isEmpty()) {
            User owner = new User();
            owner.setUsername(DEMO_OWNER_USERNAME);
            owner.setPassword(passwordEncoder.encode(ownerPassword));
            owner.setRole(UserRole.OWNER);
            owner.setTenant(tenant);
            owner.setIsActive(true);
            userRepository.save(owner);
            logger.info("Demo owner initialized: {}", DEMO_OWNER_USERNAME);
        } else {
            logger.info("Demo owner already exists: {}", DEMO_OWNER_USERNAME);
        }

        seedServices(tenant);
        seedStaff(tenant);

        if (userRepository.findByUsername(DEMO_STAFF_USERNAME).isEmpty()) {
            Staff demoStaff = staffRepository.findAllByTenantId(tenant.getId()).stream()
                    .filter(s -> "0901234567".equals(s.getPhone()))
                    .findFirst()
                    .orElse(null);

            User staff = new User();
            staff.setUsername(DEMO_STAFF_USERNAME);
            staff.setPassword(passwordEncoder.encode(staffPassword));
            staff.setRole(UserRole.STAFF);
            staff.setTenant(tenant);
            staff.setStaff(demoStaff);
            staff.setIsActive(true);
            userRepository.save(staff);
            logger.info("Demo staff initialized: {}", DEMO_STAFF_USERNAME);
        } else {
            userRepository.findByUsername(DEMO_STAFF_USERNAME).ifPresent(staffUser -> {
                if (staffUser.getStaff() == null) {
                    staffRepository.findAllByTenantId(tenant.getId()).stream()
                            .filter(s -> "0901234567".equals(s.getPhone()))
                            .findFirst()
                            .ifPresent(st -> {
                                staffUser.setStaff(st);
                                userRepository.save(staffUser);
                            });
                }
            });
            logger.info("Demo staff already exists: {}", DEMO_STAFF_USERNAME);
        }
    }

    private Tenant findLegacyOrCreateTenant() {
        return tenantRepository.findBySlug(LEGACY_TENANT_SLUG)
                .map(legacy -> {
                    legacy.setName(DEMO_TENANT_NAME);
                    legacy.setSlug(DEMO_TENANT_SLUG);
                    Tenant renamed = tenantRepository.save(legacy);
                    logger.info("Legacy tenant '{}' renamed to '{}'", LEGACY_TENANT_SLUG, DEMO_TENANT_SLUG);
                    return renamed;
                })
                .orElseGet(this::createDemoTenant);
    }

    private Tenant createDemoTenant() {
        Tenant tenant = new Tenant();
        tenant.setName(DEMO_TENANT_NAME);
        tenant.setSlug(DEMO_TENANT_SLUG);
        tenant.setIsActive(true);
        tenantRepository.save(tenant);

        logger.info("Demo tenant initialized: {}", DEMO_TENANT_SLUG);
        return tenant;
    }

    private void seedServices(Tenant tenant) {
        List<Service> existingServices = serviceRepository.findAllByTenantId(tenant.getId());

        seedServiceIfAbsent(tenant, existingServices,
                "Chăm sóc da mặt cơ bản",
                "Liệu trình chăm sóc da mặt cơ bản để duy trì sức khỏe làn da.",
                60, new BigDecimal("300000.00"));
        seedServiceIfAbsent(tenant, existingServices,
                "Massage thư giãn cơ bản",
                "Massage thư giãn toàn thân.",
                60, new BigDecimal("350000.00"));
        seedServiceIfAbsent(tenant, existingServices,
                "Làm sạch sâu da mặt",
                "Làm sạch sâu và tẩy tế bào chết.",
                90, new BigDecimal("450000.00"));
        seedServiceIfAbsent(tenant, existingServices,
                "Massage toàn thân nâng cao",
                "Massage thư giãn toàn thân.",
                60, new BigDecimal("500000.00"));
    }

    private void seedServiceIfAbsent(Tenant tenant, List<Service> existingServices, String name, String description, int durationMinutes, BigDecimal price) {
        boolean exists = existingServices.stream().anyMatch(s -> s.getName().equals(name));
        if (!exists) {
            Service s = new Service();
            s.setTenant(tenant);
            s.setName(name);
            s.setDescription(description);
            s.setDurationMinutes(durationMinutes);
            s.setPrice(price);
            s.setIsActive(true);
            serviceRepository.save(s);
            logger.info("Demo service initialized: {}", name);
        } else {
            logger.info("Demo service already exists: {}", name);
        }
    }

    private void seedStaff(Tenant tenant) {
        List<Staff> existingStaff = staffRepository.findAllByTenantId(tenant.getId());

        seedStaffIfAbsent(tenant, existingStaff, "Nguyễn An", "0901234567");
        seedStaffIfAbsent(tenant, existingStaff, "Trần Linh", "0912345678");
        seedStaffIfAbsent(tenant, existingStaff, "Lê Minh", "0923456789");
    }

    private void seedStaffIfAbsent(Tenant tenant, List<Staff> existingStaff, String name, String phone) {
        boolean exists = existingStaff.stream().anyMatch(s -> s.getPhone() != null && s.getPhone().equals(phone));
        if (!exists) {
            Staff st = new Staff();
            st.setTenant(tenant);
            st.setName(name);
            st.setPhone(phone);
            st.setIsActive(true);
            staffRepository.save(st);
            logger.info("Demo staff initialized: {}", name);
        } else {
            logger.info("Demo staff already exists: {}", name);
        }
    }
}
