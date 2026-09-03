package com.mysportsapp.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptInviteRequest(
        @NotBlank(message = "inviteToken is required") String inviteToken,
        @NotBlank(message = "password is required")
        @Size(min = 8, message = "password must be at least 8 characters") String password
) {
}
