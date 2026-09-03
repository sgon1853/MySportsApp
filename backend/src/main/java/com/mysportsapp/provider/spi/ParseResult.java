package com.mysportsapp.provider.spi;

import java.util.List;

/**
 * Everything a provider extracted from one uploaded file. As new provider
 * families are added (dive computer, body scale, ...) this record grows one
 * new list per new data shape - it is not the place for shared entity logic.
 */
public record ParseResult(
        List<ParsedActivity> activities
) {
}
