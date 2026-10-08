package com.example.spabooking.publicapi.filter;

import com.example.spabooking.tenant.context.TenantContext;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class PublicTenantContextFilter extends OncePerRequestFilter {

    private final TenantRepository tenantRepository;

    public PublicTenantContextFilter(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        // Match /api/v1/public/spas/{slug}
        if (path.startsWith("/api/v1/public/spas/")) {
            String[] parts = path.split("/");
            if (parts.length >= 6) {
                String slug = parts[5];
                Optional<Tenant> tenantOpt = tenantRepository.findBySlug(slug);
                if (tenantOpt.isPresent()) {
                    TenantContext.setTenantId(tenantOpt.get().getId());
                } else {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Spa not found");
                    return;
                }
            }
        }
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (path.startsWith("/api/v1/public/spas/")) {
                TenantContext.clear();
            }
        }
    }
}
