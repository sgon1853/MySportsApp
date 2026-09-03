package com.mysportsapp.common.exception;

/**
 * An accept-invite request referenced a token that doesn't exist, has
 * already been used, or has expired.
 */
public class InvalidInviteException extends RuntimeException {

    private final boolean expired;

    public InvalidInviteException(String message, boolean expired) {
        super(message);
        this.expired = expired;
    }

    public boolean isExpired() {
        return expired;
    }
}
