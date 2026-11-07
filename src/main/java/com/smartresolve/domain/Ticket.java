package com.smartresolve.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "support_ticket")
public class Ticket {

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 5000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TicketCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TicketStatus status;

    @Column(name = "ai_summary", nullable = false, length = 1000)
    private String aiSummary;

    @Column(name = "ai_troubleshooting", nullable = false, length = 5000)
    private String aiTroubleshooting;

    @Column(name = "ai_suggested_response", nullable = false, length = 2000)
    private String aiSuggestedResponse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private SupportTeam team;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<TicketComment> comments = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ticket_ai_context", joinColumns = @JoinColumn(name = "ticket_id"))
    @Column(name = "related_ticket_id", nullable = false)
    private final Set<UUID> relatedResolvedTicketIds = new LinkedHashSet<>();

    protected Ticket() {
    }

    public Ticket(UUID id, String title, String description, User requester, TicketCategory category,
                  TicketPriority priority, TicketStatus status, String aiSummary, String aiTroubleshooting,
                  String aiSuggestedResponse, SupportTeam team) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.requester = requester;
        this.category = category;
        this.priority = priority;
        this.status = status;
        this.aiSummary = aiSummary;
        this.aiTroubleshooting = aiTroubleshooting;
        this.aiSuggestedResponse = aiSuggestedResponse;
        this.team = team;
    }

    @PrePersist
    void setCreationTimes() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void updateTimestamp() {
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TicketCategory getCategory() {
        return category;
    }

    public void setCategory(TicketCategory category) {
        this.category = category;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }

    public String getAiTroubleshooting() {
        return aiTroubleshooting;
    }

    public void setAiTroubleshooting(String aiTroubleshooting) {
        this.aiTroubleshooting = aiTroubleshooting;
    }

    public String getAiSuggestedResponse() {
        return aiSuggestedResponse;
    }

    public void setAiSuggestedResponse(String aiSuggestedResponse) {
        this.aiSuggestedResponse = aiSuggestedResponse;
    }

    public User getRequester() {
        return requester;
    }

    public User getAssignee() {
        return assignee;
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }

    public SupportTeam getTeam() {
        return team;
    }

    public void setTeam(SupportTeam team) {
        this.team = team;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<TicketComment> getComments() {
        return List.copyOf(comments);
    }

    public Set<UUID> getRelatedResolvedTicketIds() {
        return Set.copyOf(relatedResolvedTicketIds);
    }

    public void addRelatedResolvedTicketId(UUID ticketId) {
        if (ticketId != null && !id.equals(ticketId)) {
            relatedResolvedTicketIds.add(ticketId);
        }
    }

    public void addComment(TicketComment comment) {
        comment.setTicket(this);
        comments.add(comment);
        updatedAt = LocalDateTime.now();
    }
}
