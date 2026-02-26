package com.aguiar_marcel.hyperativa_api.adapters.in.web.auth.dto;

public record AuthResponse(
        String accessToken,
        long expiresIn
) {}
