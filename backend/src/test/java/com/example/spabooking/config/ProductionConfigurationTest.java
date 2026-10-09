package com.example.spabooking.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class ProductionConfigurationTest {

    @Test
    @DisplayName("Production configuration file must exist and contain hardened production settings")
    void testProductionConfigurationProperties() throws Exception {
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application-prod.properties")) {
            assertNotNull(in, "application-prod.properties must exist on classpath");
            props.load(in);
        }

        // Profile activation
        assertEquals("prod", props.getProperty("spring.config.activate.on-profile"));

        // Hibernate ddl-auto & Flyway
        assertEquals("validate", props.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("true", props.getProperty("spring.flyway.enabled"));
        assertEquals("true", props.getProperty("spring.flyway.baseline-on-migrate"));

        // SQL Logging must be disabled
        assertEquals("false", props.getProperty("spring.jpa.show-sql"));
        assertEquals("false", props.getProperty("spring.jpa.properties.hibernate.format_sql"));

        // Graceful shutdown
        assertEquals("graceful", props.getProperty("server.shutdown"));
        assertNotNull(props.getProperty("spring.lifecycle.timeout-per-shutdown-phase"));

        // HikariCP connection pool
        assertNotNull(props.getProperty("spring.datasource.hikari.maximum-pool-size"));
        assertNotNull(props.getProperty("spring.datasource.hikari.minimum-idle"));
        assertNotNull(props.getProperty("spring.datasource.hikari.connection-timeout"));
        assertNotNull(props.getProperty("spring.datasource.hikari.pool-name"));

        // Multipart limits (aligned with 5MB app limit and 10MB Nginx limit)
        assertEquals("5MB", props.getProperty("spring.servlet.multipart.max-file-size"));
        assertEquals("10MB", props.getProperty("spring.servlet.multipart.max-request-size"));

        // Logging levels
        assertEquals("INFO", props.getProperty("logging.level.root"));
        assertEquals("WARN", props.getProperty("logging.level.org.hibernate.SQL"));

        // Trusted proxies
        assertNotNull(props.getProperty("security.rate-limit.trusted-proxies"));

        // Production bootstrap configuration
        assertNotNull(props.getProperty("bootstrap.production.enabled"));
        assertNotNull(props.getProperty("bootstrap.production.tenant-name"));
        assertNotNull(props.getProperty("bootstrap.production.tenant-slug"));
        assertNotNull(props.getProperty("bootstrap.production.owner-username"));
        assertNotNull(props.getProperty("bootstrap.production.owner-password"));

        // Production configuration must NOT contain dangerous localhost, docker container, or plaintext fallbacks
        String dsUrl = props.getProperty("spring.datasource.url");
        assertNotNull(dsUrl);
        assertEquals("${SPRING_DATASOURCE_URL}", dsUrl, "Production datasource URL must be purely environment-driven");
        assertFalse(dsUrl.contains("localhost"), "Production datasource URL must not have a localhost fallback");
        assertFalse(dsUrl.contains("mysql:"), "Production datasource URL must not hardcode Docker compose mysql service name");
        assertFalse(dsUrl.contains("useSSL=false"), "Production datasource URL must not allow plaintext connections");

        String dsUser = props.getProperty("spring.datasource.username");
        assertNotNull(dsUser);
        assertEquals("${SPRING_DATASOURCE_USERNAME}", dsUser, "Production datasource username must be environment-driven");

        String dsPass = props.getProperty("spring.datasource.password");
        assertNotNull(dsPass);
        assertEquals("${DB_PASSWORD}", dsPass, "Production datasource password must be environment-driven");

        String cors = props.getProperty("app.cors.allowed-origins");
        assertNotNull(cors);
        assertFalse(cors.contains("localhost"), "Production CORS must not have a localhost fallback");

        String vnpTmn = props.getProperty("vnpay.tmn-code");
        assertNotNull(vnpTmn);
        assertFalse(vnpTmn.contains("VMWY8Z1F"), "Production VNPay TMN code must not fall back to sandbox code");

        String vnpReturn = props.getProperty("vnpay.return-url");
        assertNotNull(vnpReturn);
        assertFalse(vnpReturn.contains("localhost"), "Production VNPay return URL must not fall back to localhost");

        String vnpPaymentUrl = props.getProperty("vnpay.payment-url");
        assertNotNull(vnpPaymentUrl);
        assertTrue(vnpPaymentUrl.contains("sandbox.vnpayment.vn"), "Production VNPay payment URL must default to Sandbox, never Live");
        assertFalse(vnpPaymentUrl.contains("https://vnpayment.vn"), "Production VNPay payment URL must never point to Live VNPay");

        String vnpQueryDrUrl = props.getProperty("vnpay.querydr-url");
        assertNotNull(vnpQueryDrUrl);
        assertTrue(vnpQueryDrUrl.contains("sandbox.vnpayment.vn"), "Production VNPay QueryDR URL must default to Sandbox, never Live");
        assertFalse(vnpQueryDrUrl.contains("https://vnpayment.vn"), "Production VNPay QueryDR URL must never point to Live VNPay");

        // Actuator health & observability configuration
        assertEquals("health", props.getProperty("management.endpoints.web.exposure.include"));
        assertEquals("true", props.getProperty("management.endpoint.health.probes.enabled"));
        assertEquals("never", props.getProperty("management.endpoint.health.show-details"));
        assertTrue(props.getProperty("management.endpoint.health.group.readiness.include").contains("db"));
        assertEquals("livenessState", props.getProperty("management.endpoint.health.group.liveness.include"));
        assertEquals("false", props.getProperty("management.info.env.enabled"));

        // Mail configuration must be purely environment-driven and contain no hardcoded credentials
        assertNotNull(props.getProperty("spring.mail.host"));
        assertTrue(props.getProperty("spring.mail.host").contains("${SPRING_MAIL_HOST"));
        assertNotNull(props.getProperty("spring.mail.username"));
        assertTrue(props.getProperty("spring.mail.username").contains("${SPRING_MAIL_USERNAME"));
        assertNotNull(props.getProperty("spring.mail.password"));
        assertTrue(props.getProperty("spring.mail.password").contains("${SPRING_MAIL_PASSWORD"));
        assertNotNull(props.getProperty("app.mail.from-address"));
        assertTrue(props.getProperty("app.mail.from-address").contains("${APP_MAIL_FROM_ADDRESS"));
    }
}
