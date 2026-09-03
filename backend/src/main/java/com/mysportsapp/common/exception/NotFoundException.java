package com.mysportsapp.common.exception;

/**
 * A requested resource does not exist - or, for a tenant-owned resource,
 * exists but belongs to a different user. The two cases are deliberately
 * indistinguishable to the caller (mapped to 404, never 403) so a response
 * never confirms another tenant's data exists.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
