package com.aguiar_marcel.hyperativa_api.adapters.in.web.dto;

import jakarta.validation.constraints.Pattern;

public record CardRequest(@Pattern(regexp = "\\d{13,19}")
                          String pan) {
}
