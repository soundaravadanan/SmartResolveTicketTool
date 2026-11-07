package com.smartresolve.service;

import com.smartresolve.domain.SupportTeam;
import com.smartresolve.domain.Ticket;
import com.smartresolve.domain.TicketComment;
import com.smartresolve.domain.TicketCommentType;
import com.smartresolve.domain.TicketStatus;
import com.smartresolve.domain.TicketStatusHistory;
import com.smartresolve.domain.User;
import com.smartresolve.domain.UserRole;
import com.smartresolve.repository.SupportTeamRepository;
import com.smartresolve.repository.TicketRepository;
import com.smartresolve.repository.TicketStatusHistoryRepository;
import com.smartresolve.repository.UserRepository;
import com.smartresolve.api.ro.ResolutionRO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final SupportTeamRepository teamRepository;
    private final TicketStatusHistoryRepository statusHistoryRepository;
    private final TicketTriageService triageService;
    private final TicketCreationService ticketCreationService;
    private final ResolvedTicketKnowledgeService knowledgeService;

    public TicketService(
            TicketRepository ticketRepository,
            UserRepository userRepository,
            SupportTeamRepository teamRepository,
            TicketStatusHistoryRepository statusHistoryRepository,
            TicketTriageService triageService,
            TicketCreationService ticketCreationService,
            ResolvedTicketKnowledgeService knowledgeService
    ) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.triageService = triageService;
        this.ticketCreationService = ticketCreationService;
        this.knowledgeService = knowledgeService;
    }

    @Transactional(readOnly = true)
    public List<Ticket> getAllTickets() {
        return ticketRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Ticket getTicketById(UUID id) {
        return requireTicket(id);
    }

    public Ticket createTicket(String title, String description) {
        var similarResolutions = knowledgeService.findSimilarResolutions(title, description);
        var recommendation = triageService.recommend(title, description, similarResolutions);
        return ticketCreationService.createTicket(title, description, recommendation, similarResolutions);
    }

    @Transactional
    public Ticket assignTicket(UUID ticketId, UUID assigneeId, UUID teamId) {
        Ticket ticket = requireTicket(ticketId);
        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignee not found"));
        if (assignee.getRole() != UserRole.SUPPORT_ENGINEER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignee must be a support engineer");
        }

        SupportTeam team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Support team not found"));

        ticket.setAssignee(assignee);
        ticket.setTeam(team);
        changeStatus(ticket, TicketStatus.ASSIGNED);
        return ticketRepository.save(ticket);
    }

    @Transactional
    public Ticket updateStatus(UUID ticketId, TicketStatus status) {
        Ticket ticket = requireTicket(ticketId);
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is required");
        }
        if (status == TicketStatus.RESOLVED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Use the resolve endpoint and provide resolution notes"
            );
        }
        changeStatus(ticket, status);
        return ticketRepository.save(ticket);
    }

    @Transactional
    public Ticket resolveTicket(UUID ticketId, String resolution) {
        Ticket ticket = requireTicket(ticketId);
        if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only open tickets can be resolved");
        }
        User author = userRepository.findFirstByRoleOrderByName(UserRole.SUPPORT_ENGINEER)
                .orElseThrow(() -> new IllegalStateException("No support engineer is available"));
        TicketComment resolutionComment = new TicketComment(author, resolution, TicketCommentType.RESOLUTION);
        ticket.addComment(resolutionComment);
        changeStatus(ticket, TicketStatus.RESOLVED);
        ticketRepository.save(ticket);
        knowledgeService.indexResolution(ticket, resolutionComment);
        return ticketRepository.findWithDetailsById(ticket.getId()).orElseThrow();
    }

    @Transactional
    public Ticket addComment(UUID ticketId, String message) {
        Ticket ticket = requireTicket(ticketId);
        User author = userRepository.findFirstByRoleOrderByName(UserRole.SUPPORT_ENGINEER)
                .orElseThrow(() -> new IllegalStateException("No support engineer is available"));
        ticket.addComment(new TicketComment(author, message, TicketCommentType.COMMENT));
        return ticketRepository.save(ticket);
    }

    @Transactional(readOnly = true)
    public Map<TicketStatus, Long> getStatusCounts() {
        Map<TicketStatus, Long> counts = new EnumMap<>(TicketStatus.class);
        for (TicketStatus status : TicketStatus.values()) {
            counts.put(status, ticketRepository.countByStatus(status));
        }
        return counts;
    }

    @Transactional(readOnly = true)
    public long getTicketCount() {
        return ticketRepository.count();
    }

    @Transactional(readOnly = true)
    public List<User> getUsers() {
        return userRepository.findAll().stream()
                .sorted((first, second) -> first.getName().compareToIgnoreCase(second.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SupportTeam> getTeams() {
        return teamRepository.findAllByOrderByName();
    }

    private Ticket requireTicket(UUID id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket ID is required");
        }
        return ticketRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
    }

    private void changeStatus(Ticket ticket, TicketStatus newStatus) {
        if (ticket.getStatus() == newStatus) {
            return;
        }
        ticket.setStatus(newStatus);
        statusHistoryRepository.save(new TicketStatusHistory(ticket, newStatus));
    }
}
