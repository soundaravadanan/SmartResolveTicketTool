package com.smartresolve.api.ro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolutionRO(
        @NotBlank(message = "Resolution notes are required")
        @Size(max = 2000, message = "Resolution notes must be 2000 characters or fewer")
        String message
) {
}
