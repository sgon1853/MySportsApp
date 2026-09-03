package com.mysportsapp.imports.dto;

import java.util.List;

public record ProviderInfoDto(
        String providerId,
        String displayName,
        List<String> supportedExtensions
) {
}
