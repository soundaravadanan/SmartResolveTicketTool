package com.smartresolve.repository;

import com.smartresolve.domain.Ticket;
import com.smartresolve.domain.TicketStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    @EntityGraph(attributePaths = {
            "requester", "assignee", "team", "comments", "comments.author", "relatedResolvedTicketIds"
    })
    List<Ticket> findAllByOrderByCreatedAtDesc();

    @Query("""
            select distinct ticket
            from Ticket ticket
            left join fetch ticket.requester
            left join fetch ticket.assignee
            left join fetch ticket.team
            left join fetch ticket.comments comment
            left join fetch comment.author
            where ticket.id = :id
            """)
    Optional<Ticket> findWithDetailsById(@Param("id") UUID id);

    long countByStatus(TicketStatus status);
}
