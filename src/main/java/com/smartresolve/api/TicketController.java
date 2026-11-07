package com.smartresolve.api;

import com.smartresolve.api.ro.AssignTicketRO;
import com.smartresolve.api.ro.CommentRO;
import com.smartresolve.api.ro.CreateTicketRO;
import com.smartresolve.api.ro.DashboardStatsRO;
import com.smartresolve.api.ro.ResolutionRO;
import com.smartresolve.api.ro.StatusUpdateRO;
import com.smartresolve.api.ro.SupportTeamRO;
import com.smartresolve.api.ro.TicketRO;
import com.smartresolve.api.ro.TicketResponseMapper;
import com.smartresolve.api.ro.UserRO;
import com.smartresolve.domain.TicketStatus;
import com.smartresolve.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class TicketController {

    private final TicketService ticketService;
    private final TicketResponseMapper mapper;

    public TicketController(TicketService ticketService, TicketResponseMapper mapper) {
        this.ticketService = ticketService;
        this.mapper = mapper;
    }

    @GetMapping("/tickets")
    public List<TicketRO> getTickets() {
        return ticketService.getAllTickets().stream()
                .map(mapper::toTicketRO)
                .toList();
    }

    @PostMapping("/tickets")
    public ResponseEntity<TicketRO> createTicket(@Valid @RequestBody CreateTicketRO request) {
        var ticket = ticketService.createTicket(request.title(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toTicketRO(ticket));
    }

    @GetMapping("/tickets/{id}")
    public TicketRO getTicket(@PathVariable UUID id) {
        return mapper.toTicketRO(ticketService.getTicketById(id));
    }

    @PostMapping("/tickets/{id}/assign")
    public TicketRO assignTicket(@PathVariable UUID id, @Valid @RequestBody AssignTicketRO request) {
        var ticket = ticketService.assignTicket(id, request.assigneeId(), request.teamId());
        return mapper.toTicketRO(ticket);
    }

    @PatchMapping("/tickets/{id}/status")
    public TicketRO updateStatus(@PathVariable UUID id, @Valid @RequestBody StatusUpdateRO request) {
        var ticket = ticketService.updateStatus(id, request.status());
        return mapper.toTicketRO(ticket);
    }

    @PostMapping("/tickets/{id}/comments")
    public TicketRO addComment(@PathVariable UUID id, @Valid @RequestBody CommentRO request) {
        var ticket = ticketService.addComment(id, request.message());
        return mapper.toTicketRO(ticket);
    }

    @PostMapping("/tickets/{id}/resolve")
    public TicketRO resolveTicket(@PathVariable UUID id, @Valid @RequestBody ResolutionRO request) {
        var ticket = ticketService.resolveTicket(id, request.message());
        return mapper.toTicketRO(ticket);
    }

    @GetMapping("/dashboard/stats")
    public DashboardStatsRO getDashboardStats() {
        Map<TicketStatus, Long> counts = ticketService.getStatusCounts();
        return new DashboardStatsRO(
                countFor(counts, TicketStatus.NEW),
                countFor(counts, TicketStatus.ASSIGNED),
                countFor(counts, TicketStatus.IN_PROGRESS),
                countFor(counts, TicketStatus.RESOLVED),
                countFor(counts, TicketStatus.CLOSED),
                ticketService.getTicketCount()
        );
    }

    @GetMapping("/users")
    public List<UserRO> getUsers() {
        return ticketService.getUsers().stream()
                .map(mapper::toUserRO)
                .toList();
    }

    @GetMapping("/teams")
    public List<SupportTeamRO> getTeams() {
        return ticketService.getTeams().stream()
                .map(mapper::toTeamRO)
                .toList();
    }

    private long countFor(Map<TicketStatus, Long> counts, TicketStatus status) {
        return counts.getOrDefault(status, 0L);
    }
}
