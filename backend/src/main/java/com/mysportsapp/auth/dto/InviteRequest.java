package com.mysportsapp.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InviteRequest(
        @NotBlank(message = "email is required")
        @Email(message = "email must be valid") String email
) {
}
