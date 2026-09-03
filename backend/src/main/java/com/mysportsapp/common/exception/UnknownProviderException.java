package com.mysportsapp.common.exception;

/**
 * An import was requested with a {@code providerId} that isn't registered
 * in the {@code DataProviderRegistry}.
 */
public class UnknownProviderException extends RuntimeException {

    public UnknownProviderException(String providerId) {
        super("Unknown provider id: " + providerId);
    }
}
