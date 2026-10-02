package com.example.spabooking.tenant.filter;

import com.example.spabooking.auth.entity.User;
import com.example.spabooking.auth.security.CustomUserDetails;
import com.example.spabooking.tenant.context.TenantContext;
import com.example.spabooking.tenant.entity.Tenant;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TenantContextFilterTest {

    private TenantContextFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new TenantContextFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void testAuthenticatedUserWithTenantIsResolved() throws ServletException, IOException {
        Tenant tenant = new Tenant();
        tenant.setId(10L);
        User user = new User();
        user.setUsername("testuser");
        user.setRole(com.example.spabooking.auth.enums.UserRole.OWNER);
        user.setTenant(tenant);
        CustomUserDetails userDetails = new CustomUserDetails(user);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        FilterChain chain = (req, res) -> assertEquals(10L, TenantContext.getTenantId());

        filter.doFilterInternal(request, response, chain);

        assertNull(TenantContext.getTenantId(), "Context should be cleared after request");
    }

    @Test
    void testAuthenticatedUserWithTenantA() throws ServletException, IOException {
        Tenant tenant = new Tenant();
        tenant.setId(100L);
        User user = new User();
        user.setUsername("userA");
        user.setRole(com.example.spabooking.auth.enums.UserRole.OWNER);
        user.setTenant(tenant);
        CustomUserDetails userDetails = new CustomUserDetails(user);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        FilterChain chain = (req, res) -> assertEquals(100L, TenantContext.getTenantId());

        filter.doFilterInternal(request, response, chain);
        assertNull(TenantContext.getTenantId());
    }

    @Test
    void testAuthenticatedUserWithTenantB() throws ServletException, IOException {
        Tenant tenant = new Tenant();
        tenant.setId(200L);
        User user = new User();
        user.setUsername("userB");
        user.setRole(com.example.spabooking.auth.enums.UserRole.OWNER);
        user.setTenant(tenant);
        CustomUserDetails userDetails = new CustomUserDetails(user);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        FilterChain chain = (req, res) -> assertEquals(200L, TenantContext.getTenantId());

        filter.doFilterInternal(request, response, chain);
        assertNull(TenantContext.getTenantId());
    }

    @Test
    void testUnauthenticatedRequest() throws ServletException, IOException {
        FilterChain chain = (req, res) -> assertNull(TenantContext.getTenantId());

        filter.doFilterInternal(request, response, chain);

        assertNull(TenantContext.getTenantId());
    }

    @Test
    void testAuthenticatedUserWithoutTenant() throws ServletException, IOException {
        User user = new User();
        user.setUsername("notenantuser");
        user.setRole(com.example.spabooking.auth.enums.UserRole.OWNER);
        user.setTenant(null); // No tenant
        CustomUserDetails userDetails = new CustomUserDetails(user);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        FilterChain chain = (req, res) -> assertNull(TenantContext.getTenantId()); // Should not fallback or set anything

        filter.doFilterInternal(request, response, chain);

        assertNull(TenantContext.getTenantId());
    }

    @Test
    void testContextClearedAfterException() {
        Tenant tenant = new Tenant();
        tenant.setId(50L);
        User user = new User();
        user.setUsername("exceptionuser");
        user.setRole(com.example.spabooking.auth.enums.UserRole.OWNER);
        user.setTenant(tenant);
        CustomUserDetails userDetails = new CustomUserDetails(user);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        FilterChain chain = (req, res) -> {
            assertEquals(50L, TenantContext.getTenantId());
            throw new ServletException("Simulated exception");
        };

        try {
            filter.doFilterInternal(request, response, chain);
        } catch (Exception e) {
            // Expected
        }

        assertNull(TenantContext.getTenantId(), "Context must be cleared even if exception occurs");
    }
}
