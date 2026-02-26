package com.aguiar_marcel.hyperativa_api.adapters.in.web.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
        @JsonProperty("login") @NotBlank String login,
        @JsonProperty("password") @NotBlank String password
) {}
