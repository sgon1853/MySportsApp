package com.mysportsapp.security;

import com.mysportsapp.user.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * The Spring Security principal for every authenticated request. Carries the
 * user's id, which is the ONLY source of truth for "who is making this
 * request" - controllers and services must read the tenant id from here via
 * {@link org.springframework.security.core.context.SecurityContextHolder},
 * never from a path variable, query param, or request body field.
 */
public record AuthenticatedUser(UUID id, String email, UserRole role) {

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
