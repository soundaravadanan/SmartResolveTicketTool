package com.smartresolve.api.ro;

import java.time.LocalDateTime;
import java.util.UUID;
import com.smartresolve.domain.TicketCommentType;

public record TicketCommentRO(
        UUID id,
        UserRO author,
        String message,
        TicketCommentType type,
        LocalDateTime createdAt
) {
}
