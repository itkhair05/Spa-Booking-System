package com.example.spabooking.tenant.filter;

import com.example.spabooking.auth.security.CustomUserDetails;
import com.example.spabooking.tenant.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantContextFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(TenantContextFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // Public endpoints resolve their tenant from the URL slug; a caller's JWT
        // must not override that resolution.
        if (request.getRequestURI().startsWith("/api/v1/public/spas/")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                
                if (userDetails.getUser() != null && userDetails.getUser().getTenant() != null) {
                    Long tenantId = userDetails.getUser().getTenant().getId();
                    TenantContext.setTenantId(tenantId);
                    logger.debug("Resolved tenantId {} for user {}", tenantId, userDetails.getUsername());
                } else {
                    logger.warn("User {} is authenticated but has no tenant associated.", userDetails.getUsername());
                }
            }
            
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
