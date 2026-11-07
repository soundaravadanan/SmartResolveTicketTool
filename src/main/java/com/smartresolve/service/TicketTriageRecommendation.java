package com.smartresolve.service;

import com.smartresolve.domain.TicketCategory;
import com.smartresolve.domain.TicketPriority;

import java.util.List;

public record TicketTriageRecommendation(
        String summary,
        TicketCategory category,
        TicketPriority priority,
        List<String> troubleshootingSteps,
        String suggestedResponse
) {
}
