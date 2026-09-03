package com.mysportsapp.provider.spi;

import java.io.InputStream;
import java.util.Set;

/**
 * Service-provider interface for a single import source (a watch, a dive
 * computer, an Apple Health export, a smart scale, ...).
 *
 * <p>To add support for a new data source: implement this interface in its
 * own {@code com.mysportsapp.provider.<name>} package, annotate it
 * {@code @Component}, and pick a unique {@link #getProviderId()}. Spring
 * picks it up automatically via {@code DataProviderRegistry} - nothing
 * elsewhere in the app needs to change.
 *
 * <p>Implementations must be pure: given the same bytes they must produce
 * the same {@link ParseResult}, with no database access, no Spring-managed
 * repositories, and no other side effects. This keeps every provider
 * unit-testable with zero Spring context.
 */
public interface DataProvider {

    /**
     * Stable, unique identifier for this provider (e.g. {@code "suunto-gpx"}).
     * Used as the {@code providerId} in the import API and persisted on
     * every activity it produces.
     */
    String getProviderId();

    /**
     * Human-readable name for display in the frontend's provider picker.
     */
    String getDisplayName();

    /**
     * File extensions (without the leading dot, lower-case) this provider
     * is able to handle, e.g. {@code Set.of("gpx")}.
     */
    Set<String> getSupportedFileExtensions();

    /**
     * Cheap pre-check on the filename and/or leading bytes of the content,
     * used before committing to a full parse.
     */
    boolean canParse(String filename, byte[] content);

    /**
     * Parses the given file content into structured activity data.
     *
     * @throws ProviderParseException if the content cannot be parsed
     */
    ParseResult parse(InputStream input, ImportContext context) throws ProviderParseException;
}
