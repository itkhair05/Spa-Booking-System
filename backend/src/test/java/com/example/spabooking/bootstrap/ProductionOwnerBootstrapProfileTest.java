package com.example.spabooking.bootstrap;

import com.example.spabooking.auth.repository.UserRepository;
import com.example.spabooking.tenant.repository.TenantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ProductionOwnerBootstrapProfileTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(ProductionOwnerBootstrapSeeder.class)
            .withBean(TenantRepository.class, () -> mock(TenantRepository.class))
            .withBean(UserRepository.class, () -> mock(UserRepository.class))
            .withBean(PasswordEncoder.class, () -> mock(PasswordEncoder.class));

    @Test
    @DisplayName("Seeder bean is NOT registered when 'prod' profile is not active (e.g. dev or default)")
    void seederBeanIsNotRegisteredOutsideProdProfile() {
        runner.withPropertyValues("spring.profiles.active=default")
                .run(context -> assertThat(context).doesNotHaveBean(ProductionOwnerBootstrapSeeder.class));

        runner.withPropertyValues("spring.profiles.active=dev")
                .run(context -> assertThat(context).doesNotHaveBean(ProductionOwnerBootstrapSeeder.class));
    }

    @Test
    @DisplayName("Seeder bean IS registered when 'prod' profile is active and disabled by default")
    void seederBeanIsRegisteredUnderProdProfile() {
        runner.withPropertyValues("spring.profiles.active=prod")
                .run(context -> {
                    assertThat(context).hasSingleBean(ProductionOwnerBootstrapSeeder.class);
                    ProductionOwnerBootstrapSeeder seeder = context.getBean(ProductionOwnerBootstrapSeeder.class);
                    assertThat(seeder.isEnabled()).isFalse();
                });
    }

    @Test
    @DisplayName("Seeder bean reflects enabled property when PRODUCTION_BOOTSTRAP_ENABLED=true")
    void seederBeanReflectsEnabledProperty() {
        runner.withPropertyValues("spring.profiles.active=prod", "bootstrap.production.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(ProductionOwnerBootstrapSeeder.class);
                    ProductionOwnerBootstrapSeeder seeder = context.getBean(ProductionOwnerBootstrapSeeder.class);
                    assertThat(seeder.isEnabled()).isTrue();
                });
    }
}
