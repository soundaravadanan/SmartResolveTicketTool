package com.smartresolve.api.ro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRO(
        @NotBlank(message = "Comment is required")
        @Size(max = 2000, message = "Comment must be 2000 characters or fewer")
        String message
) {
}
