package com.example.spabooking.auth.security;

import com.example.spabooking.auth.entity.User;
import com.example.spabooking.auth.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        // Set properties via reflection since it's not a Spring Boot Test
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", "8z0sD3q123L5j7B4h8W1a6v8O0p9Q4y2Z9n7K6f3v8A5c7T0d8X4n2J0w5T3h7Y");
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 3600000); // 1 hour
    }

    @Test
    void testGenerateAndValidateJwtToken() {
        User user = new User();
        user.setUsername("testuser");
        user.setRole(UserRole.OWNER);
        CustomUserDetails userDetails = new CustomUserDetails(user);

        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        String token = jwtUtils.generateJwtToken(auth);

        assertNotNull(token);
        assertTrue(jwtUtils.validateJwtToken(token));
        assertEquals("testuser", jwtUtils.getUserNameFromJwtToken(token));
    }

    @Test
    void testValidateJwtToken_Invalid() {
        assertFalse(jwtUtils.validateJwtToken("invalid.token.here"));
    }

    @Test
    void testValidateJwtToken_TamperedSignature() {
        User user = new User();
        user.setUsername("testuser");
        user.setRole(UserRole.OWNER);
        CustomUserDetails userDetails = new CustomUserDetails(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        String token = jwtUtils.generateJwtToken(auth);
        char lastChar = token.charAt(token.length() - 1);
        char replacement = lastChar == 'A' ? 'B' : 'A';
        String tamperedToken = token.substring(0, token.length() - 1) + replacement;

        assertFalse(jwtUtils.validateJwtToken(tamperedToken));
    }

    @Test
    void testValidateJwtToken_TamperedClaims() {
        User user = new User();
        user.setUsername("testuser");
        user.setRole(UserRole.OWNER);
        CustomUserDetails userDetails = new CustomUserDetails(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        String token = jwtUtils.generateJwtToken(auth);
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);

        // Flip the first character of the payload segment; the original signature stays valid for the old payload only
        char original = parts[1].charAt(0);
        char different = original == 'A' ? 'B' : 'A';
        String tamperedPayload = different + parts[1].substring(1);
        String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

        assertFalse(jwtUtils.validateJwtToken(tamperedToken));
    }

    @Test
    void testValidateJwtToken_Expired() throws InterruptedException {
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 1); // 1 ms expiration

        User user = new User();
        user.setUsername("testuser");
        user.setRole(UserRole.OWNER);
        CustomUserDetails userDetails = new CustomUserDetails(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        String token = jwtUtils.generateJwtToken(auth);

        // Sleep to ensure token expires
        Thread.sleep(10);

        assertFalse(jwtUtils.validateJwtToken(token));
    }
}
