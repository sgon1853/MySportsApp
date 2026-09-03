package com.mysportsapp.provider;

import com.mysportsapp.provider.spi.DataProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Indexes every {@link DataProvider} bean in the application context by its
 * {@code providerId}. Adding a new provider is exactly: implement
 * {@link DataProvider}, annotate it {@code @Component}, done - Spring
 * injects it here automatically.
 */
@Component
public class DataProviderRegistry {

    private final Map<String, DataProvider> providersById;

    public DataProviderRegistry(List<DataProvider> providers) {
        this.providersById = providers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        DataProvider::getProviderId,
                        Function.identity(),
                        (a, b) -> {
                            throw new IllegalStateException(
                                    "Duplicate DataProvider id '" + a.getProviderId() + "' registered by "
                                            + a.getClass().getName() + " and " + b.getClass().getName());
                        }));
    }

    public Optional<DataProvider> findById(String providerId) {
        return Optional.ofNullable(providersById.get(providerId));
    }

    public List<DataProvider> listAll() {
        return List.copyOf(providersById.values());
    }
}
