package com.finpilot.user.web.dto;

public record AuthResponse(
        String accessToken, Long userId, String email, String name, String role) {
}