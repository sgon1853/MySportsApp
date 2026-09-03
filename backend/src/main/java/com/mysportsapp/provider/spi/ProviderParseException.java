package com.mysportsapp.provider.spi;

/**
 * Thrown by a {@link DataProvider} when a file it was asked to parse cannot
 * be understood (malformed, corrupt, or not actually of the claimed type).
 * Unchecked so provider implementations don't need boilerplate try/catch,
 * and so {@code GlobalExceptionHandler} can map it directly to a 422.
 */
public class ProviderParseException extends RuntimeException {

    public ProviderParseException(String message) {
        super(message);
    }

    public ProviderParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
