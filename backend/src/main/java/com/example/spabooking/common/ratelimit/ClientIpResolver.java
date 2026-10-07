package com.example.spabooking.common.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Resolves the authoritative client IP address for rate limiting and security auditing.
 * Distinguishes between trusted proxies (e.g. Cloudflare, local Nginx, private Docker bridge)
 * and untrusted direct clients to prevent header-spoofing attacks.
 */
@Component
public class ClientIpResolver {

    private static final Logger log = LoggerFactory.getLogger(ClientIpResolver.class);

    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^(?:(?:25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])\\.){3}(?:25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])$"
    );

    private static final Pattern IPV6_PATTERN = Pattern.compile(
            "^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$|" +
            "^::(?:[0-9a-fA-F]{1,4}:){0,6}[0-9a-fA-F]{1,4}$|" +
            "^(?:[0-9a-fA-F]{1,4}:){1,7}:$|" +
            "^(?:[0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}$|" +
            "^(?:[0-9a-fA-F]{1,4}:){1,5}(?::[0-9a-fA-F]{1,4}){1,2}$|" +
            "^(?:[0-9a-fA-F]{1,4}:){1,4}(?::[0-9a-fA-F]{1,4}){1,3}$|" +
            "^(?:[0-9a-fA-F]{1,4}:){1,3}(?::[0-9a-fA-F]{1,4}){1,4}$|" +
            "^(?:[0-9a-fA-F]{1,4}:){1,2}(?::[0-9a-fA-F]{1,4}){1,5}$|" +
            "^[0-9a-fA-F]{1,4}:(?::[0-9a-fA-F]{1,4}){1,6}$|" +
            "^:(?::[0-9a-fA-F]{1,4}){1,7}$|^::$"
    );

    public static final String DEFAULT_TRUSTED_PROXIES =
            "127.0.0.1,::1,0:0:0:0:0:0:0:1,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16";

    private final List<String> exactTrustedIps = new ArrayList<>();
    private final List<Ipv4Subnet> trustedSubnets = new ArrayList<>();

    public ClientIpResolver() {
        this(DEFAULT_TRUSTED_PROXIES);
    }

    @Autowired
    public ClientIpResolver(
            @Value("${security.rate-limit.trusted-proxies:" + DEFAULT_TRUSTED_PROXIES + "}") String trustedProxiesConfig) {
        parseTrustedProxies(trustedProxiesConfig != null ? trustedProxiesConfig : DEFAULT_TRUSTED_PROXIES);
    }

    private void parseTrustedProxies(String config) {
        Arrays.stream(config.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(entry -> {
                    if (entry.contains("/")) {
                        try {
                            trustedSubnets.add(new Ipv4Subnet(entry));
                        } catch (Exception ex) {
                            log.warn("Invalid CIDR format in trusted proxies: {}", entry);
                        }
                    } else {
                        exactTrustedIps.add(normalizeIp(entry));
                    }
                });
    }

    /**
     * Resolves the real client IP.
     * If the immediate connection does not come from a trusted proxy, proxy headers are
     * discarded to prevent spoofing.
     */
    public String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String remoteAddr = normalizeIp(request.getRemoteAddr());
        if (remoteAddr == null || remoteAddr.isBlank()) {
            return "unknown";
        }

        // If the immediate client is untrusted, NEVER trust forwarding headers.
        if (!isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }

        // 1. Cloudflare authoritative client IP
        String cfConnectingIp = request.getHeader("CF-Connecting-IP");
        if (cfConnectingIp != null && !cfConnectingIp.isBlank()) {
            String candidate = cleanIp(cfConnectingIp);
            if (isValidIp(candidate)) {
                return candidate;
            }
        }

        // 2. Nginx real IP header
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            String candidate = cleanIp(xRealIp);
            if (isValidIp(candidate)) {
                return candidate;
            }
        }

        // 3. Standard X-Forwarded-For (take the first client IP)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            String firstIp = cleanIp(xForwardedFor.split(",")[0]);
            if (isValidIp(firstIp)) {
                return firstIp;
            }
        }

        // Fallback to the immediate socket address
        return remoteAddr;
    }

    public boolean isTrustedProxy(String remoteAddr) {
        if (remoteAddr == null || remoteAddr.isBlank()) {
            return false;
        }
        String normalized = normalizeIp(remoteAddr);

        // Check exact match (e.g. 127.0.0.1, ::1)
        for (String trusted : exactTrustedIps) {
            if (trusted.equalsIgnoreCase(normalized)) {
                return true;
            }
        }

        // Check IPv4 CIDR subnets
        if (IPV4_PATTERN.matcher(normalized).matches()) {
            for (Ipv4Subnet subnet : trustedSubnets) {
                if (subnet.matches(normalized)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean isValidIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        String clean = cleanIp(ip);
        return IPV4_PATTERN.matcher(clean).matches() || IPV6_PATTERN.matcher(clean).matches();
    }

    private static String normalizeIp(String ip) {
        if (ip == null) return null;
        String trimmed = ip.trim();
        if (trimmed.startsWith("::ffff:") && trimmed.length() > 7) {
            String sub = trimmed.substring(7);
            if (IPV4_PATTERN.matcher(sub).matches()) {
                return sub;
            }
        }
        return cleanIp(trimmed);
    }

    private static String cleanIp(String ip) {
        if (ip == null) return null;
        String s = ip.trim();
        // Remove port if IPv4 address has port: e.g. 1.2.3.4:8080
        if (s.contains(":") && s.indexOf(':') == s.lastIndexOf(':')) {
            String[] parts = s.split(":");
            if (IPV4_PATTERN.matcher(parts[0]).matches()) {
                return parts[0];
            }
        }
        // Remove brackets if [IPv6]:port or [IPv6]
        if (s.startsWith("[") && s.contains("]")) {
            return s.substring(1, s.indexOf(']'));
        }
        return s;
    }

    private static final class Ipv4Subnet {
        private final int network;
        private final int mask;

        Ipv4Subnet(String cidr) {
            String[] parts = cidr.split("/");
            int ip = parseIpv4ToInt(parts[0].trim());
            int prefix = Integer.parseInt(parts[1].trim());
            if (prefix < 0 || prefix > 32) {
                throw new IllegalArgumentException("Invalid prefix: " + prefix);
            }
            this.mask = prefix == 0 ? 0 : 0xFFFFFFFF << (32 - prefix);
            this.network = ip & mask;
        }

        boolean matches(String ipStr) {
            try {
                int ip = parseIpv4ToInt(ipStr);
                return (ip & mask) == network;
            } catch (Exception ex) {
                return false;
            }
        }

        private static int parseIpv4ToInt(String ip) {
            String[] octets = ip.split("\\.");
            if (octets.length != 4) {
                throw new IllegalArgumentException("Invalid IPv4: " + ip);
            }
            int result = 0;
            for (int i = 0; i < 4; i++) {
                int octet = Integer.parseInt(octets[i]);
                if (octet < 0 || octet > 255) {
                    throw new IllegalArgumentException("Invalid octet: " + octet);
                }
                result = (result << 8) | octet;
            }
            return result;
        }
    }
}
