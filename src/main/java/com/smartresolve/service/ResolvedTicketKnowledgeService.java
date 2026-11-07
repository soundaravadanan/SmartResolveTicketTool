package com.smartresolve.service;

import com.smartresolve.domain.Ticket;
import com.smartresolve.domain.TicketComment;
import com.smartresolve.domain.TicketCommentType;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ResolvedTicketKnowledgeService {

    private static final String TICKET_ID_METADATA = "ticketId";
    private static final String TICKET_TITLE_METADATA = "ticketTitle";
    private static final String RESOLUTION_METADATA = "resolution";
    private static final String LEGACY_RESOLUTION_MARKER = "Resolution:";

    private final VectorStore vectorStore;
    private final double similarityThreshold;

    public ResolvedTicketKnowledgeService(
            VectorStore vectorStore,
            @Value("${smartresolve.knowledge.similarity-threshold:0.75}") double similarityThreshold
    ) {
        this.vectorStore = vectorStore;
        this.similarityThreshold = similarityThreshold;
    }

    public List<ResolvedTicketContext> findSimilarResolutions(String title, String description) {
        String query = "Ticket title: " + title + "\nTicket description: " + description;

        return vectorStore.similaritySearch(SearchRequest.builder()
                        .query(query)
                        .topK(3)
                        .similarityThreshold(similarityThreshold)
                        .build())
                .stream()
                .map(this::toContext)
                .toList();
    }

    public void indexResolution(Ticket ticket, TicketComment resolution) {
        if (resolution.getType() != TicketCommentType.RESOLUTION) {
            throw new IllegalArgumentException("Only resolution notes can be added to ticket knowledge");
        }

        String text = """
                Resolved support ticket
                Title: %s
                Category: %s
                Issue: %s
                """.formatted(
                ticket.getTitle(),
                ticket.getCategory(),
                ticket.getDescription()
        );

        Document document = new Document(
                ticket.getId().toString(),
                text,
                Map.of(
                        TICKET_ID_METADATA, ticket.getId().toString(),
                        TICKET_TITLE_METADATA, ticket.getTitle(),
                        RESOLUTION_METADATA, resolution.getMessage()
                )
        );
        vectorStore.add(List.of(document));
    }

    private ResolvedTicketContext toContext(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        Object ticketId = metadata.get(TICKET_ID_METADATA);
        Object ticketTitle = metadata.get(TICKET_TITLE_METADATA);
        Object resolution = metadata.get(RESOLUTION_METADATA);
        if (ticketId == null || ticketTitle == null) {
            throw new IllegalStateException("Stored resolved-ticket context is missing required metadata");
        }
        String resolutionText = resolution == null
                ? extractLegacyResolution(document.getText())
                : resolution.toString();

        return new ResolvedTicketContext(
                UUID.fromString(ticketId.toString()),
                ticketTitle.toString(),
                resolutionText
        );
    }

    private String extractLegacyResolution(String documentText) {
        if (documentText == null) {
            throw new IllegalStateException("Stored resolved-ticket context has no resolution text");
        }

        int resolutionStart = documentText.indexOf(LEGACY_RESOLUTION_MARKER);
        if (resolutionStart < 0) {
            throw new IllegalStateException("Stored resolved-ticket context has no resolution text");
        }

        return documentText.substring(resolutionStart + LEGACY_RESOLUTION_MARKER.length()).trim();
    }
}
