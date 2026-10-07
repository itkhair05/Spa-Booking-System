package com.example.spabooking.bootstrap;

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

import java.util.Optional;

/**
 * Production bootstrap component for initializing the initial Tenant and OWNER account.
 * Activated strictly under the 'prod' profile and runs only when explicitly enabled
 * via PRODUCTION_BOOTSTRAP_ENABLED=true.
 * 
 * Never logs credentials or password hashes. Safe and idempotent against repeated runs.
 */
@Component
@Profile("prod")
public class ProductionOwnerBootstrapSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ProductionOwnerBootstrapSeeder.class);

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final boolean enabled;
    private final String tenantName;
    private final String tenantSlug;
    private final String tenantPhone;
    private final String tenantEmail;
    private final String tenantAddress;
    private final String tenantTimezone;
    private final String ownerUsername;
    private final String ownerPassword;

    public ProductionOwnerBootstrapSeeder(
            TenantRepository tenantRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${bootstrap.production.enabled:false}") boolean enabled,
            @Value("${bootstrap.production.tenant-name:}") String tenantName,
            @Value("${bootstrap.production.tenant-slug:}") String tenantSlug,
            @Value("${bootstrap.production.tenant-phone:}") String tenantPhone,
            @Value("${bootstrap.production.tenant-email:}") String tenantEmail,
            @Value("${bootstrap.production.tenant-address:}") String tenantAddress,
            @Value("${bootstrap.production.tenant-timezone:Asia/Ho_Chi_Minh}") String tenantTimezone,
            @Value("${bootstrap.production.owner-username:}") String ownerUsername,
            @Value("${bootstrap.production.owner-password:}") String ownerPassword) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.tenantName = tenantName != null ? tenantName.trim() : "";
        this.tenantSlug = tenantSlug != null ? tenantSlug.trim() : "";
        this.tenantPhone = tenantPhone != null ? tenantPhone.trim() : "";
        this.tenantEmail = tenantEmail != null ? tenantEmail.trim() : "";
        this.tenantAddress = tenantAddress != null ? tenantAddress.trim() : "";
        this.tenantTimezone = (tenantTimezone != null && !tenantTimezone.isBlank()) ? tenantTimezone.trim() : "Asia/Ho_Chi_Minh";
        this.ownerUsername = ownerUsername != null ? ownerUsername.trim() : "";
        this.ownerPassword = ownerPassword != null ? ownerPassword.trim() : "";
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) {
            logger.debug("Production bootstrap is disabled (PRODUCTION_BOOTSTRAP_ENABLED=false).");
            return;
        }

        logger.info("Production bootstrap is enabled. Validating configuration...");
        validateConfiguration();

        logger.info("Executing production bootstrap for tenant slug: '{}'...", tenantSlug);
        executeBootstrap();
        logger.info("Production bootstrap process finished.");
    }

    private void validateConfiguration() {
        if (tenantName.isBlank()) {
            throw new IllegalStateException("Production bootstrap failed: BOOTSTRAP_TENANT_NAME must be provided and cannot be blank.");
        }
        if (tenantSlug.isBlank()) {
            throw new IllegalStateException("Production bootstrap failed: BOOTSTRAP_TENANT_SLUG must be provided and cannot be blank.");
        }
        if (ownerUsername.isBlank()) {
            throw new IllegalStateException("Production bootstrap failed: BOOTSTRAP_OWNER_USERNAME must be provided and cannot be blank.");
        }
        if (ownerPassword.isBlank()) {
            throw new IllegalStateException("Production bootstrap failed: BOOTSTRAP_OWNER_PASSWORD must be provided and cannot be blank.");
        }
        if (ownerPassword.length() < 8) {
            throw new IllegalStateException("Production bootstrap failed: BOOTSTRAP_OWNER_PASSWORD must be at least 8 characters long.");
        }
    }

    private void executeBootstrap() {
        Optional<Tenant> existingTenantOpt = tenantRepository.findBySlug(tenantSlug);
        Optional<User> existingUserOpt = userRepository.findByUsername(ownerUsername);

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            boolean tenantMatches = existingTenantOpt.isPresent()
                    && existingUser.getTenant() != null
                    && existingUser.getTenant().getId().equals(existingTenantOpt.get().getId());

            if (!tenantMatches) {
                throw new IllegalStateException(
                        "Production bootstrap failed: User '" + ownerUsername + "' already exists and belongs to a different tenant. "
                                + "Cannot bootstrap as OWNER for tenant '" + tenantSlug + "'.");
            }

            if (existingUser.getRole() != UserRole.OWNER) {
                throw new IllegalStateException(
                        "Production bootstrap failed: User '" + ownerUsername + "' already exists in tenant '" + tenantSlug
                                + "' with role '" + existingUser.getRole() + "' and cannot be bootstrapped as OWNER.");
            }

            logger.info("Production bootstrap skipped: Tenant with slug '{}' and OWNER user '{}' already exist.", tenantSlug, ownerUsername);
            return;
        }

        Tenant tenant = existingTenantOpt.orElseGet(() -> {
            Tenant newTenant = new Tenant();
            newTenant.setName(tenantName);
            newTenant.setSlug(tenantSlug);
            if (!tenantPhone.isBlank()) {
                newTenant.setPhone(tenantPhone);
            }
            if (!tenantEmail.isBlank()) {
                newTenant.setEmail(tenantEmail);
            }
            if (!tenantAddress.isBlank()) {
                newTenant.setAddress(tenantAddress);
            }
            newTenant.setTimezone(tenantTimezone);
            newTenant.setIsActive(true);
            Tenant saved = tenantRepository.save(newTenant);
            logger.info("Production bootstrap: created Tenant '{}' (slug: '{}').", saved.getName(), saved.getSlug());
            return saved;
        });

        User owner = new User();
        owner.setUsername(ownerUsername);
        owner.setPassword(passwordEncoder.encode(ownerPassword));
        owner.setRole(UserRole.OWNER);
        owner.setTenant(tenant);
        owner.setIsActive(true);
        userRepository.save(owner);
        logger.info("Production bootstrap: created OWNER user '{}' for tenant '{}'.", ownerUsername, tenant.getSlug());
    }


    public boolean isEnabled() {
        return enabled;
    }
}
