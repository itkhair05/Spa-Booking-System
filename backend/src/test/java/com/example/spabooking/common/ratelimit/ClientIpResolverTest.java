package com.example.spabooking.common.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class ClientIpResolverTest {

    private final ClientIpResolver resolver = new ClientIpResolver();

    @Test
    @DisplayName("Direct request from public IP with no proxy headers resolves to socket remoteAddr")
    void directRequestResolvesToRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");

        String clientIp = resolver.resolveClientIp(request);
        assertEquals("203.0.113.10", clientIp);
    }

    @Test
    @DisplayName("Trusted proxy (127.0.0.1) with CF-Connecting-IP resolves to real client IP")
    void trustedProxyWithCfConnectingIpResolvesRealClientIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("CF-Connecting-IP", "203.0.113.50");

        String clientIp = resolver.resolveClientIp(request);
        assertEquals("203.0.113.50", clientIp);
    }

    @Test
    @DisplayName("Trusted proxy in Docker private subnet (172.18.0.2) with X-Real-IP resolves to client IP")
    void dockerProxyWithXRealIpResolvesClientIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.18.0.2");
        request.addHeader("X-Real-IP", "198.51.100.77");

        String clientIp = resolver.resolveClientIp(request);
        assertEquals("198.51.100.77", clientIp);
    }

    @Test
    @DisplayName("Trusted proxy with X-Forwarded-For resolves to leftmost client IP")
    void trustedProxyWithXForwardedForResolvesLeftmostIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.1.5");
        request.addHeader("X-Forwarded-For", "203.0.113.99, 10.0.1.1, 10.0.1.5");

        String clientIp = resolver.resolveClientIp(request);
        assertEquals("203.0.113.99", clientIp);
    }

    @Test
    @DisplayName("Untrusted direct client attempting to spoof CF-Connecting-IP is ignored")
    void untrustedClientSpoofingCfConnectingIpIsIgnored() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.22"); // Public/untrusted IP
        request.addHeader("CF-Connecting-IP", "1.2.3.4"); // Spoofed header

        String clientIp = resolver.resolveClientIp(request);
        // Must NOT trust 1.2.3.4; must use the actual connection IP
        assertEquals("198.51.100.22", clientIp);
    }

    @Test
    @DisplayName("Untrusted direct client attempting to spoof X-Forwarded-For is ignored")
    void untrustedClientSpoofingXForwardedForIsIgnored() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.33"); // Public/untrusted IP
        request.addHeader("X-Forwarded-For", "5.6.7.8, 10.0.0.1"); // Spoofed header

        String clientIp = resolver.resolveClientIp(request);
        assertEquals("198.51.100.33", clientIp);
    }

    @Test
    @DisplayName("Untrusted direct client attempting to spoof X-Real-IP is ignored")
    void untrustedClientSpoofingXRealIpIsIgnored() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.44");
        request.addHeader("X-Real-IP", "9.9.9.9");

        String clientIp = resolver.resolveClientIp(request);
        assertEquals("198.51.100.44", clientIp);
    }

    @Test
    @DisplayName("Invalid IP format in header is rejected and falls back safely to remoteAddr")
    void invalidIpFormatInHeaderFallsBackToRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("CF-Connecting-IP", "../../etc/passwd");

        String clientIp = resolver.resolveClientIp(request);
        assertEquals("127.0.0.1", clientIp);
    }

    @Test
    @DisplayName("IPv4 with port syntax is cleaned properly")
    void ipv4WithPortIsCleaned() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("CF-Connecting-IP", "203.0.113.88:443");

        String clientIp = resolver.resolveClientIp(request);
        assertEquals("203.0.113.88", clientIp);
    }

    @Test
    @DisplayName("IPv6 loopback is recognized as trusted proxy")
    void ipv6LoopbackIsTrusted() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("::1");
        request.addHeader("CF-Connecting-IP", "203.0.113.123");

        String clientIp = resolver.resolveClientIp(request);
        assertEquals("203.0.113.123", clientIp);
    }

    @Test
    @DisplayName("Custom trusted proxy list works as configured")
    void customTrustedProxyListWorks() {
        ClientIpResolver customResolver = new ClientIpResolver("192.0.2.1, 198.51.100.0/24");

        // 192.0.2.1 is trusted in this custom resolver
        MockHttpServletRequest req1 = new MockHttpServletRequest();
        req1.setRemoteAddr("192.0.2.1");
        req1.addHeader("CF-Connecting-IP", "1.1.1.1");
        assertEquals("1.1.1.1", customResolver.resolveClientIp(req1));

        // 198.51.100.50 is trusted (in 198.51.100.0/24)
        MockHttpServletRequest req2 = new MockHttpServletRequest();
        req2.setRemoteAddr("198.51.100.50");
        req2.addHeader("CF-Connecting-IP", "2.2.2.2");
        assertEquals("2.2.2.2", customResolver.resolveClientIp(req2));

        // 127.0.0.1 is NOT in this custom list, so it is treated as untrusted
        MockHttpServletRequest req3 = new MockHttpServletRequest();
        req3.setRemoteAddr("127.0.0.1");
        req3.addHeader("CF-Connecting-IP", "3.3.3.3");
        assertEquals("127.0.0.1", customResolver.resolveClientIp(req3));
    }
}
