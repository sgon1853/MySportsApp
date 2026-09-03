package com.mysportsapp.common.exception;

/**
 * Login failed: unknown email, wrong password, or an inactive account.
 * Deliberately generic so responses never confirm whether an email exists.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
