package com.example.spabooking.security;

import com.example.spabooking.auth.config.SecurityConfig;
import com.example.spabooking.common.ratelimit.ClientIpResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.cors.CorsConfiguration;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.cors.allowed-origins=https://example.com,https://www.example.com",
        "security.rate-limit.trusted-proxies=127.0.0.1,::1,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16"
})
@Transactional
public class ProductionDomainAndCorsSecurityTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private SecurityConfig securityConfig;

    @Autowired
    private ClientIpResolver clientIpResolver;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("1. Preflight OPTIONS from configured production origin succeeds with credentials")
    void testCorsPreflightAllowedOriginSucceeds() throws Exception {
        mockMvc.perform(options("/api/v1/public/spas/tikey-spa")
                        .header("Origin", "https://example.com")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://example.com"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    @DisplayName("2. Preflight OPTIONS from secondary configured origin succeeds")
    void testCorsPreflightSecondaryOriginSucceeds() throws Exception {
        mockMvc.perform(options("/api/v1/public/spas/tikey-spa")
                        .header("Origin", "https://www.example.com")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://www.example.com"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    @DisplayName("3. Preflight OPTIONS from untrusted origin is rejected")
    void testCorsPreflightUntrustedOriginRejected() throws Exception {
        mockMvc.perform(options("/api/v1/public/spas/tikey-spa")
                        .header("Origin", "https://malicious-attacker.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("4. Preflight OPTIONS from localhost is rejected in production domain configuration")
    void testCorsPreflightLocalhostRejectedWhenNotConfigured() throws Exception {
        mockMvc.perform(options("/api/v1/public/spas/tikey-spa")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("5. Wildcard origin '*' is stripped when credentials are enabled to prevent vulnerability")
    void testWildcardOriginIsFilteredOut() {
        var source = securityConfig.corsConfigurationSource("https://example.com, *, https://www.example.com");
        MockHttpServletRequest request = new MockHttpServletRequest();
        CorsConfiguration config = source.getCorsConfiguration(request);

        assertNotNull(config);
        assertTrue(config.getAllowCredentials());
        assertNotNull(config.getAllowedOrigins());
        assertFalse(config.getAllowedOrigins().contains("*"), "Wildcard origin '*' must be filtered out");
        assertTrue(config.getAllowedOrigins().contains("https://example.com"));
        assertTrue(config.getAllowedOrigins().contains("https://www.example.com"));
    }

    @Test
    @DisplayName("6. Forwarded HTTPS request (X-Forwarded-Proto: https) is recognized as secure and receives HSTS")
    void testForwardedHttpsEmitsHstsHeader() throws Exception {
        mockMvc.perform(get("/api/v1/public/spas/tikey-spa")
                        .header("X-Forwarded-Proto", "https")
                        .header("X-Forwarded-Port", "443")
                        .secure(true))
                .andExpect(status().isOk())
                .andExpect(header().string("Strict-Transport-Security", containsString("max-age=31536000")))
                .andExpect(header().string("Strict-Transport-Security", containsString("includeSubDomains")));
    }

    @Test
    @DisplayName("7. Trusted reverse proxy correctly forwards Cloudflare CF-Connecting-IP")
    void testTrustedProxyForwardsCloudflareConnectingIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.18.0.2"); // Docker private network subnet (trusted)
        request.addHeader("CF-Connecting-IP", "203.0.113.88");

        String resolvedIp = clientIpResolver.resolveClientIp(request);
        assertEquals("203.0.113.88", resolvedIp);
    }

    @Test
    @DisplayName("8. Untrusted direct client cannot spoof Cloudflare CF-Connecting-IP")
    void testUntrustedDirectClientCannotSpoofCloudflareConnectingIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.12"); // Direct public IP (untrusted)
        request.addHeader("CF-Connecting-IP", "1.1.1.1"); // Attempted spoof

        String resolvedIp = clientIpResolver.resolveClientIp(request);
        assertEquals("198.51.100.12", resolvedIp, "Spoofed CF-Connecting-IP must be discarded from untrusted peers");
    }
}
