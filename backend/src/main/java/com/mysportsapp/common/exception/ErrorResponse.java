package com.mysportsapp.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Uniform error response shape returned by {@link GlobalExceptionHandler}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String message, List<String> details) {

    public ErrorResponse(String message) {
        this(message, null);
    }
}
