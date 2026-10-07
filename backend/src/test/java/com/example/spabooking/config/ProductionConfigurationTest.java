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

        // Production configuration must NOT contain dangerous localhost or sandbox fallbacks
        String dsUrl = props.getProperty("spring.datasource.url");
        assertNotNull(dsUrl);
        assertFalse(dsUrl.contains("localhost"), "Production datasource URL must not have a localhost fallback");

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
        assertFalse(vnpPaymentUrl.contains("sandbox"), "Production VNPay payment URL must not fall back to sandbox");
    }
}
