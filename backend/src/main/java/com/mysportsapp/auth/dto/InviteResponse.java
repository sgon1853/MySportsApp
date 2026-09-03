package com.mysportsapp.auth.dto;

public record InviteResponse(String email, String inviteToken, String expiresAt) {
}
