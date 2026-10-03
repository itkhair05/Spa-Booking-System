package com.example.spabooking.auth.config;

import com.example.spabooking.auth.security.AuthEntryPointJwt;
import com.example.spabooking.auth.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthEntryPointJwt unauthorizedHandler;
    private final com.example.spabooking.tenant.filter.TenantContextFilter tenantContextFilter;
    private final com.example.spabooking.publicapi.filter.PublicTenantContextFilter publicTenantContextFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter, 
                          AuthEntryPointJwt unauthorizedHandler,
                          com.example.spabooking.tenant.filter.TenantContextFilter tenantContextFilter,
                          com.example.spabooking.publicapi.filter.PublicTenantContextFilter publicTenantContextFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.unauthorizedHandler = unauthorizedHandler;
        this.tenantContextFilter = tenantContextFilter;
        this.publicTenantContextFilter = publicTenantContextFilter;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/login").permitAll()
                .requestMatchers("/api/v1/public/**").permitAll()
                .anyRequest().authenticated()
            );

        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(publicTenantContextFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(tenantContextFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
