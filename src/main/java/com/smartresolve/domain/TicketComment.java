package com.smartresolve.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ticket_comment")
public class TicketComment {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 2000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "comment_type", nullable = false, length = 20)
    private TicketCommentType type;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected TicketComment() {
    }

    public TicketComment(UUID id, User author, String message, LocalDateTime createdAt) {
        this(id, author, message, TicketCommentType.COMMENT, createdAt);
    }

    public TicketComment(UUID id, User author, String message, TicketCommentType type, LocalDateTime createdAt) {
        this.id = id;
        this.author = author;
        this.message = message;
        this.type = type;
        this.createdAt = createdAt;
    }

    public TicketComment(User author, String message) {
        this(author, message, TicketCommentType.COMMENT);
    }

    public TicketComment(User author, String message, TicketCommentType type) {
        this(UUID.randomUUID(), author, message, type, LocalDateTime.now());
    }

    void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    @PrePersist
    void setCreatedAtIfMissing() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public User getAuthor() {
        return author;
    }

    public String getMessage() {
        return message;
    }

    public TicketCommentType getType() {
        return type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
