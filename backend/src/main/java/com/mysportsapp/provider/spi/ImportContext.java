package com.mysportsapp.provider.spi;

import java.util.UUID;

/**
 * Contextual metadata handed to a {@link DataProvider} for a single parse
 * call. Deliberately inert: providers must not use this to reach into the
 * database or any Spring-managed component - it exists purely so a provider
 * can, if it wants, stamp identifiers into its output. Providers stay pure
 * {@code bytes -> DTOs} functions so they're unit-testable with zero Spring
 * context.
 */
public record ImportContext(
        UUID userId,
        String originalFilename,
        UUID batchId
) {
}
