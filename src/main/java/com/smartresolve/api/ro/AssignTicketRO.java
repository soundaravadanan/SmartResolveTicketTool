package com.smartresolve.api.ro;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignTicketRO(
        @NotNull(message = "Assignee is required") UUID assigneeId,
        @NotNull(message = "Support team is required") UUID teamId
) {
}
