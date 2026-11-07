package com.smartresolve.api.ro;

import com.smartresolve.domain.TicketCategory;
import com.smartresolve.domain.TicketPriority;
import com.smartresolve.domain.TicketStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TicketRO(
        UUID id,
        String title,
        String description,
        TicketCategory category,
        TicketPriority priority,
        TicketStatus status,
        String aiSummary,
        String aiTroubleshooting,
        String aiSuggestedResponse,
        UserRO requester,
        UserRO assignee,
        SupportTeamRO team,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<TicketCommentRO> comments,
        List<RelatedResolvedTicketRO> relatedResolvedTickets
) {
}
