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
@Table(name = "ticket_status_history")
public class TicketStatusHistory {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TicketStatus status;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    protected TicketStatusHistory() {
    }

    public TicketStatusHistory(Ticket ticket, TicketStatus status) {
        this.id = UUID.randomUUID();
        this.ticket = ticket;
        this.status = status;
        this.changedAt = LocalDateTime.now();
    }

    @PrePersist
    void setChangedAtIfMissing() {
        if (changedAt == null) {
            changedAt = LocalDateTime.now();
        }
    }
}
