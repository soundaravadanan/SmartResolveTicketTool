package com.smartresolve.api.ro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTicketRO(
        @NotBlank(message = "Title is required")
        @Size(max = 120, message = "Title must be 120 characters or fewer")
        String title,
        @NotBlank(message = "Description is required")
        @Size(max = 5000, message = "Description must be 5000 characters or fewer")
        String description
) {
}
