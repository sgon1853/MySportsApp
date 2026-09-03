package com.mysportsapp.user;

/**
 * Roles a {@link User} can hold. USER accounts only ever see their own data;
 * ADMIN accounts can additionally invite new users.
 */
public enum UserRole {
    USER,
    ADMIN
}
