package com.smartresolve.api.ro;

import com.smartresolve.domain.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRO(@NotNull(message = "Status is required") TicketStatus status) {
}
