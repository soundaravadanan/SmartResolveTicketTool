package com.smartresolve.api;

import com.smartresolve.domain.Ticket;
import com.smartresolve.domain.TicketCategory;
import com.smartresolve.domain.TicketPriority;
import com.smartresolve.domain.TicketStatus;
import com.smartresolve.domain.UserRole;
import com.smartresolve.repository.TicketStatusHistoryRepository;
import com.smartresolve.service.TicketService;
import com.smartresolve.service.TicketTriageRecommendation;
import com.smartresolve.service.TicketTriageException;
import com.smartresolve.service.TicketTriageService;
import com.smartresolve.service.ResolvedTicketKnowledgeService;
import com.smartresolve.service.ResolvedTicketContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketStatusHistoryRepository statusHistoryRepository;

    @MockitoBean
    private TicketTriageService triageService;

    @MockitoBean
    private ResolvedTicketKnowledgeService knowledgeService;

    @BeforeEach
    void setUp() {
        when(knowledgeService.findSimilarResolutions(anyString(), anyString())).thenReturn(List.of());
        when(triageService.recommend(anyString(), anyString(), anyList())).thenReturn(
                new TicketTriageRecommendation(
                        "Sample access issue",
                        TicketCategory.ACCESS_ISSUE,
                        TicketPriority.HIGH,
                        List.of("Check the account status"),
                        "Support is reviewing the issue."
                )
        );
        ticketService.createTicket("Payroll login", "Invalid credentials");
    }

    @Test
    void shouldReturnTicketResponseObjects() throws Exception {
        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requester.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$[0].aiSummary").isNotEmpty());
    }

    @Test
    void shouldReturnNotFoundForUnknownTicket() throws Exception {
        mockMvc.perform(get("/api/tickets/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket not found"));
    }

    @Test
    void shouldReturnReadableValidationError() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\" \",\"description\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void shouldRejectAssignmentToEmployeeWithReadableError() throws Exception {
        Ticket ticket = ticketService.getAllTickets().get(0);
        var employee = ticketService.getUsers().stream()
                .filter(user -> user.getRole() == UserRole.EMPLOYEE)
                .findFirst()
                .orElseThrow();
        var team = ticketService.getTeams().get(0);
        String requestBody = """
                {
                  "assigneeId": "%s",
                  "teamId": "%s"
                }
                """.formatted(employee.getId(), team.getId());

        mockMvc.perform(post("/api/tickets/{id}/assign", ticket.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Assignee must be a support engineer"));
    }

    @Test
    void shouldReturnReadableErrorForInvalidStatusValue() throws Exception {
        Ticket ticket = ticketService.getAllTickets().get(0);

        mockMvc.perform(patch("/api/tickets/{id}/status", ticket.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"WAITING\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is invalid"));
    }

    @Test
    void shouldPersistStatusChangesAndHistory() throws Exception {
        Ticket ticket = ticketService.getAllTickets().get(0);
        long historyCount = statusHistoryRepository.count();

        mockMvc.perform(patch("/api/tickets/{id}/status", ticket.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        assertEquals(historyCount + 1, statusHistoryRepository.count());
        assertEquals(TicketStatus.IN_PROGRESS, ticketService.getTicketById(ticket.getId()).getStatus());
    }

    @Test
    void shouldSaveResolutionNotesAndMarkTicketResolved() throws Exception {
        Ticket ticket = ticketService.getAllTickets().get(0);

        mockMvc.perform(post("/api/tickets/{id}/resolve", ticket.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Unlocked the account and confirmed access.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.comments[0].type").value("RESOLUTION"))
                .andExpect(jsonPath("$.comments[0].message").value("Unlocked the account and confirmed access."));
    }

    @Test
    void shouldRejectResolvingWithoutNotes() throws Exception {
        Ticket ticket = ticketService.getAllTickets().get(0);

        mockMvc.perform(post("/api/tickets/{id}/resolve", ticket.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void shouldIncludeResolvedTicketContextOnNewTicket() throws Exception {
        Ticket sourceTicket = ticketService.getAllTickets().get(0);
        String resolution = "Reset the account credentials and verified the employee can sign in.";
        mockMvc.perform(post("/api/tickets/{id}/resolve", sourceTicket.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"" + resolution + "\"}"))
                .andExpect(status().isOk());

        when(knowledgeService.findSimilarResolutions("Payroll sign-in", "Cannot access payroll"))
                .thenReturn(List.of(new ResolvedTicketContext(
                        sourceTicket.getId(),
                        sourceTicket.getTitle(),
                        "Resolution: " + resolution
                )));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Payroll sign-in",
                                  "description": "Cannot access payroll"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.relatedResolvedTickets[0].id").value(sourceTicket.getId().toString()))
                .andExpect(jsonPath("$.relatedResolvedTickets[0].title").value(sourceTicket.getTitle()))
                .andExpect(jsonPath("$.relatedResolvedTickets[0].resolution").value(resolution));
    }

    @Test
    void shouldReturnServiceUnavailableWhenAiTriageFails() throws Exception {
        doThrow(new TicketTriageException("Ollama is unavailable"))
                .when(triageService).recommend("AI unavailable", "Please categorize this ticket", List.of());

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "AI unavailable",
                                  "description": "Please categorize this ticket"
                                }
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Ollama is unavailable"));
    }

    @Test
    void shouldExplainWhenAiOutputDoesNotMeetRequirements() throws Exception {
        doThrow(new TicketTriageException(
                "AI recommendation did not include troubleshooting steps",
                org.springframework.http.HttpStatus.BAD_GATEWAY
        )).when(triageService).recommend("Missing steps", "No troubleshooting details returned", List.of());

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Missing steps",
                                  "description": "No troubleshooting details returned"
                                }
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("AI recommendation did not include troubleshooting steps"));
    }
}
