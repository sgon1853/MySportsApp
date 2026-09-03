package com.mysportsapp.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * {@link User} IS the tenant boundary (not a tenant-owned entity), so
 * lookups here are intentionally not scoped by a separate tenant id.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByInviteToken(String inviteToken);

    boolean existsByRole(UserRole role);
}
