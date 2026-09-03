package com.mysportsapp.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A MySportsApp account. {@code User} is itself the tenant boundary: every
 * other tenant-owned entity (activities, import batches, ...) is scoped by
 * {@code user.id}, so there is no separate "tenant" concept to model.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "invited_by")
    private UUID invitedBy;

    @Column(name = "invite_token")
    private String inviteToken;

    @Column(name = "invite_token_expires_at")
    private Instant inviteTokenExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected User() {
        // JPA
    }

    public User(UUID id, String email, String passwordHash, UserRole role, boolean active,
                UUID invitedBy, String inviteToken, Instant inviteTokenExpiresAt, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
        this.invitedBy = invitedBy;
        this.inviteToken = inviteToken;
        this.inviteTokenExpiresAt = inviteTokenExpiresAt;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public UUID getInvitedBy() {
        return invitedBy;
    }

    public String getInviteToken() {
        return inviteToken;
    }

    public void setInviteToken(String inviteToken) {
        this.inviteToken = inviteToken;
    }

    public Instant getInviteTokenExpiresAt() {
        return inviteTokenExpiresAt;
    }

    public void setInviteTokenExpiresAt(Instant inviteTokenExpiresAt) {
        this.inviteTokenExpiresAt = inviteTokenExpiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
