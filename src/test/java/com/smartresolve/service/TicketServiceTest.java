package com.smartresolve.service;

import com.smartresolve.domain.SupportTeam;
import com.smartresolve.domain.Ticket;
import com.smartresolve.domain.TicketCategory;
import com.smartresolve.domain.TicketCommentType;
import com.smartresolve.domain.TicketPriority;
import com.smartresolve.domain.TicketStatus;
import com.smartresolve.domain.User;
import com.smartresolve.domain.UserRole;
import com.smartresolve.repository.SupportTeamRepository;
import com.smartresolve.repository.TicketRepository;
import com.smartresolve.repository.TicketStatusHistoryRepository;
import com.smartresolve.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SupportTeamRepository teamRepository;
    @Mock
    private TicketStatusHistoryRepository statusHistoryRepository;
    @Mock
    private TicketTriageService triageService;
    @Mock
    private TicketCreationService ticketCreationService;
    @Mock
    private ResolvedTicketKnowledgeService knowledgeService;

    private TicketService service;
    private User employee;
    private User engineer;
    private SupportTeam team;

    @BeforeEach
    void setUp() {
        service = new TicketService(
                ticketRepository,
                userRepository,
                teamRepository,
                statusHistoryRepository,
                triageService,
                ticketCreationService,
                knowledgeService
        );
        employee = new User("Priya Sharma", "priya@company.com", UserRole.EMPLOYEE);
        engineer = new User("Arjun Mehta", "arjun@company.com", UserRole.SUPPORT_ENGINEER);
        team = new SupportTeam("Identity & Access", "Handles access issues");
    }

    @Test
    void shouldSendRecommendationToTicketCreationService() {
        TicketTriageRecommendation recommendation = recommendation();
        when(triageService.recommend("Payroll login", "Invalid credentials", java.util.List.of()))
                .thenReturn(recommendation);
        Ticket expected = ticket();
        when(ticketCreationService.createTicket(
                "Payroll login",
                "Invalid credentials",
                recommendation,
                java.util.List.of()
        ))
                .thenReturn(expected);

        Ticket created = service.createTicket("Payroll login", "Invalid credentials");

        assertEquals(expected, created);
        verify(ticketCreationService).createTicket(
                "Payroll login",
                "Invalid credentials",
                recommendation,
                java.util.List.of()
        );
    }

    @Test
    void shouldNotSaveTicketWhenAiRecommendationFails() {
        when(triageService.recommend("Issue", "Details", java.util.List.of()))
                .thenThrow(new IllegalStateException("AI provider unavailable"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.createTicket("Issue", "Details")
        );

        assertEquals("AI provider unavailable", exception.getMessage());
        verify(ticketCreationService, never()).createTicket(any(), any(), any(), anyList());
    }

    @Test
    void shouldRequireResolutionNotesBeforeResolvingAndIndexTheResolution() {
        Ticket ticket = ticket();
        when(ticketRepository.findWithDetailsById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(userRepository.findFirstByRoleOrderByName(UserRole.SUPPORT_ENGINEER))
                .thenReturn(Optional.of(engineer));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        Ticket resolved = service.resolveTicket(ticket.getId(), "Unlocked the account and confirmed access.");

        assertEquals(TicketStatus.RESOLVED, resolved.getStatus());
        assertEquals(TicketCommentType.RESOLUTION, resolved.getComments().get(0).getType());
        verify(knowledgeService).indexResolution(ticket, resolved.getComments().get(0));
    }

    @Test
    void shouldNotAllowStatusUpdateToResolvedWithoutResolutionNotes() {
        Ticket ticket = ticket();
        when(ticketRepository.findWithDetailsById(ticket.getId())).thenReturn(Optional.of(ticket));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.updateStatus(ticket.getId(), TicketStatus.RESOLVED)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void shouldRejectAssignmentToNonEngineer() {
        Ticket ticket = ticket();
        when(ticketRepository.findWithDetailsById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(userRepository.findById(employee.getId())).thenReturn(Optional.of(employee));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.assignTicket(ticket.getId(), employee.getId(), team.getId())
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    private Ticket ticket() {
        return new Ticket(
                UUID.randomUUID(),
                "Payroll login",
                "Invalid credentials",
                employee,
                TicketCategory.ACCESS_ISSUE,
                TicketPriority.HIGH,
                TicketStatus.NEW,
                "Access issue",
                "Check account status",
                "We are looking into this.",
                team
        );
    }

    private TicketTriageRecommendation recommendation() {
        return new TicketTriageRecommendation(
                "Access issue",
                TicketCategory.ACCESS_ISSUE,
                TicketPriority.HIGH,
                java.util.List.of("Check account status"),
                "We are looking into this."
        );
    }
}
