package com.smartresolve.service;

import com.smartresolve.domain.SupportTeam;
import com.smartresolve.domain.Ticket;
import com.smartresolve.domain.TicketCategory;
import com.smartresolve.domain.TicketComment;
import com.smartresolve.domain.TicketCommentType;
import com.smartresolve.domain.TicketPriority;
import com.smartresolve.domain.TicketStatus;
import com.smartresolve.domain.User;
import com.smartresolve.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResolvedTicketKnowledgeServiceTest {

    private final VectorStore vectorStore = mock(VectorStore.class);
    private ResolvedTicketKnowledgeService service;

    @BeforeEach
    void setUp() {
        service = new ResolvedTicketKnowledgeService(vectorStore, 0.75);
    }

    @Test
    void shouldSearchWithThresholdAndConvertMatchingDocumentsIntoContext() {
        UUID ticketId = UUID.randomUUID();
        Document document = new Document(
                ticketId.toString(),
                "Resolved password reset",
                Map.of(
                        "ticketId", ticketId.toString(),
                        "ticketTitle", "Password reset",
                        "resolution", "Reset the password and confirmed sign-in."
                )
        );
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(document));

        List<ResolvedTicketContext> contexts =
                service.findSimilarResolutions("Cannot sign in", "Password reset needed");

        assertEquals(1, contexts.size());
        assertEquals(ticketId, contexts.get(0).ticketId());
        assertEquals("Password reset", contexts.get(0).title());
        assertEquals("Reset the password and confirmed sign-in.", contexts.get(0).resolution());

        ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(requestCaptor.capture());
        assertEquals(3, requestCaptor.getValue().getTopK());
        assertEquals(0.75, requestCaptor.getValue().getSimilarityThreshold());
    }

    @Test
    void shouldReturnNoContextWhenNoDocumentsMeetTheSimilarityThreshold() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        List<ResolvedTicketContext> contexts =
                service.findSimilarResolutions("API outage", "API returns HTTP 500");

        assertTrue(contexts.isEmpty());
    }

    @Test
    void shouldReadResolutionFromExistingVectorDocuments() {
        UUID ticketId = UUID.randomUUID();
        Document legacyDocument = new Document(
                ticketId.toString(),
                """
                        Resolved support ticket
                        Title: Password reset
                        Category: PASSWORD_RESET
                        Original description: Cannot sign in
                        Resolution: Reset the password and confirmed sign-in.
                        """,
                Map.of("ticketId", ticketId.toString(), "ticketTitle", "Password reset")
        );
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(legacyDocument));

        List<ResolvedTicketContext> contexts =
                service.findSimilarResolutions("Cannot sign in", "Password reset needed");

        assertEquals("Reset the password and confirmed sign-in.", contexts.get(0).resolution());
    }

    @Test
    void shouldIndexResolutionWithTicketDetails() {
        User requester = new User("Priya Sharma", "priya@company.com", UserRole.EMPLOYEE);
        SupportTeam team = new SupportTeam("Identity & Access", "Handles access issues");
        Ticket ticket = new Ticket(
                UUID.randomUUID(),
                "Payroll login",
                "Cannot sign in after password expiry",
                requester,
                TicketCategory.PASSWORD_RESET,
                TicketPriority.MEDIUM,
                TicketStatus.RESOLVED,
                "Password reset request",
                "Confirm account identity",
                "Support is reviewing the issue.",
                team
        );
        TicketComment resolution = new TicketComment(
                requester,
                "Reset the password and verified access.",
                TicketCommentType.RESOLUTION
        );
        AtomicReference<List<Document>> indexedDocuments = new AtomicReference<>();
        doAnswer(invocation -> {
            indexedDocuments.set(invocation.getArgument(0));
            return null;
        }).when(vectorStore).add(anyList());

        service.indexResolution(ticket, resolution);

        verify(vectorStore).add(anyList());
        Document indexedDocument = indexedDocuments.get().get(0);
        assertEquals(ticket.getId().toString(), indexedDocument.getId());
        assertEquals(ticket.getId().toString(), indexedDocument.getMetadata().get("ticketId"));
        assertEquals(ticket.getTitle(), indexedDocument.getMetadata().get("ticketTitle"));
        assertEquals(resolution.getMessage(), indexedDocument.getMetadata().get("resolution"));
        assertTrue(indexedDocument.getText().contains(ticket.getDescription()));
        assertFalse(indexedDocument.getText().contains(resolution.getMessage()));
    }

    @Test
    void shouldNotIndexOrdinaryCommentsAsResolutionKnowledge() {
        Ticket ticket = new Ticket(
                UUID.randomUUID(),
                "Payroll login",
                "Cannot sign in",
                new User("Priya Sharma", "priya@company.com", UserRole.EMPLOYEE),
                TicketCategory.ACCESS_ISSUE,
                TicketPriority.HIGH,
                TicketStatus.IN_PROGRESS,
                "Access issue",
                "Check account status",
                "Support is reviewing the issue.",
                new SupportTeam("Identity & Access", "Handles access issues")
        );
        TicketComment comment = new TicketComment(new User("Arjun Mehta", "arjun@company.com",
                UserRole.SUPPORT_ENGINEER), "Still investigating.");

        assertThrows(IllegalArgumentException.class, () -> service.indexResolution(ticket, comment));
    }
}
