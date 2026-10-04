package com.example.spabooking.auth.security;

import com.example.spabooking.auth.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        boolean userActive = user.getIsActive() != null && user.getIsActive();
        if (!userActive) {
            return false;
        }
        if (user.getStaff() != null && !Boolean.TRUE.equals(user.getStaff().getIsActive())) {
            return false;
        }
        return true;
    }

    public User getUser() {
        return user;
    }

    public Long getStaffId() {
        return user.getStaff() != null ? user.getStaff().getId() : null;
    }

    public boolean isStaff() {
        return user.getRole() == com.example.spabooking.auth.enums.UserRole.STAFF;
    }

    public boolean isOwner() {
        return user.getRole() == com.example.spabooking.auth.enums.UserRole.OWNER;
    }
}
