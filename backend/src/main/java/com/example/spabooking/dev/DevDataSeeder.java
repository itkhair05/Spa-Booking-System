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
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

    static final String DEMO_TENANT_NAME = "Demo Spa";
    static final String DEMO_TENANT_SLUG = "demo-spa";
    static final String DEMO_OWNER_USERNAME = "owner@demo.local";
    static final String DEMO_STAFF_USERNAME = "staff@demo.local";

    private static final Logger logger = LoggerFactory.getLogger(DevDataSeeder.class);

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String ownerPassword;
    private final String staffPassword;

    public DevDataSeeder(TenantRepository tenantRepository,
                         UserRepository userRepository,
                         PasswordEncoder passwordEncoder,
                         @Value("${dev.seed.owner-password}") String ownerPassword,
                         @Value("${dev.seed.staff-password}") String staffPassword) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
                .orElseGet(this::createDemoTenant);

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

        if (userRepository.findByUsername(DEMO_STAFF_USERNAME).isEmpty()) {
            User staff = new User();
            staff.setUsername(DEMO_STAFF_USERNAME);
            staff.setPassword(passwordEncoder.encode(staffPassword));
            staff.setRole(UserRole.STAFF);
            staff.setTenant(tenant);
            staff.setIsActive(true);
            userRepository.save(staff);
            logger.info("Demo staff initialized: {}", DEMO_STAFF_USERNAME);
        } else {
            logger.info("Demo staff already exists: {}", DEMO_STAFF_USERNAME);
        }
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
}
