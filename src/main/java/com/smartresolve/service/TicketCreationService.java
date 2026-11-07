package com.smartresolve.service;

import com.smartresolve.domain.SupportTeam;
import com.smartresolve.domain.Ticket;
import com.smartresolve.domain.TicketCategory;
import com.smartresolve.domain.TicketStatus;
import com.smartresolve.domain.TicketStatusHistory;
import com.smartresolve.domain.User;
import com.smartresolve.domain.UserRole;
import com.smartresolve.repository.SupportTeamRepository;
import com.smartresolve.repository.TicketRepository;
import com.smartresolve.repository.TicketStatusHistoryRepository;
import com.smartresolve.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;

@Service
public class TicketCreationService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final SupportTeamRepository teamRepository;
    private final TicketStatusHistoryRepository statusHistoryRepository;
    private final Map<TicketCategory, String> teamNamesByCategory = new EnumMap<>(TicketCategory.class);

    public TicketCreationService(
            TicketRepository ticketRepository,
            UserRepository userRepository,
            SupportTeamRepository teamRepository,
            TicketStatusHistoryRepository statusHistoryRepository
    ) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.statusHistoryRepository = statusHistoryRepository;

        teamNamesByCategory.put(TicketCategory.ACCESS_ISSUE, "Identity & Access");
        teamNamesByCategory.put(TicketCategory.PASSWORD_RESET, "Identity & Access");
        teamNamesByCategory.put(TicketCategory.APPLICATION_ERROR, "Application Support");
        teamNamesByCategory.put(TicketCategory.SYSTEM_INCIDENT, "Application Support");
        teamNamesByCategory.put(TicketCategory.SOFTWARE_INSTALLATION, "Application Support");
        teamNamesByCategory.put(TicketCategory.NETWORK_FAILURE, "Infrastructure Ops");
        teamNamesByCategory.put(TicketCategory.OTHER, "Application Support");
    }

    @Transactional
    public Ticket createTicket(
            String title,
            String description,
            TicketTriageRecommendation recommendation,
            List<ResolvedTicketContext> similarResolutions
    ) {
        User requester = userRepository.findFirstByRoleOrderByName(UserRole.EMPLOYEE)
                .orElseThrow(() -> new IllegalStateException("No employee is available to submit the ticket"));
        SupportTeam team = findTeamFor(recommendation.category());

        Ticket ticket = new Ticket(
                UUID.randomUUID(),
                title,
                description,
                requester,
                recommendation.category(),
                recommendation.priority(),
                TicketStatus.NEW,
                recommendation.summary(),
                String.join("\n", recommendation.troubleshootingSteps()),
                recommendation.suggestedResponse(),
                team
        );
        similarResolutions.stream()
                .map(ResolvedTicketContext::ticketId)
                .forEach(ticket::addRelatedResolvedTicketId);

        ticketRepository.save(ticket);
        statusHistoryRepository.save(new TicketStatusHistory(ticket, TicketStatus.NEW));
        return ticketRepository.findWithDetailsById(ticket.getId()).orElseThrow();
    }

    private SupportTeam findTeamFor(TicketCategory category) {
        String teamName = teamNamesByCategory.get(category);
        if (teamName == null) {
            throw new IllegalStateException("No support team is configured for " + category);
        }
        return teamRepository.findByName(teamName)
                .orElseThrow(() -> new IllegalStateException("Support team is not configured: " + teamName));
    }
}
