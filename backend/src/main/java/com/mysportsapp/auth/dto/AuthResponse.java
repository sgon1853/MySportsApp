package com.mysportsapp.auth.dto;

public record AuthResponse(String token, UserDto user) {
}
