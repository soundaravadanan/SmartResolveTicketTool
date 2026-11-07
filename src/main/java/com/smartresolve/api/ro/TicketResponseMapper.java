package com.smartresolve.api.ro;

import com.smartresolve.domain.SupportTeam;
import com.smartresolve.domain.Ticket;
import com.smartresolve.domain.TicketComment;
import com.smartresolve.domain.TicketCommentType;
import com.smartresolve.domain.User;
import com.smartresolve.repository.TicketRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class TicketResponseMapper {

    private final TicketRepository ticketRepository;

    public TicketResponseMapper(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public TicketRO toTicketRO(Ticket ticket) {
        List<TicketCommentRO> comments = ticket.getComments().stream()
                .map(this::toCommentRO)
                .toList();
        List<RelatedResolvedTicketRO> relatedTickets = ticket.getRelatedResolvedTicketIds().stream()
                .map(ticketRepository::findWithDetailsById)
                .flatMap(Optional::stream)
                .map(this::toRelatedResolvedTicketRO)
                .toList();

        return new TicketRO(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getCategory(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getAiSummary(),
                ticket.getAiTroubleshooting(),
                ticket.getAiSuggestedResponse(),
                toUserRO(ticket.getRequester()),
                toUserRO(ticket.getAssignee()),
                toTeamRO(ticket.getTeam()),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                comments,
                relatedTickets
        );
    }

    public UserRO toUserRO(User user) {
        if (user == null) {
            return null;
        }
        return new UserRO(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    public SupportTeamRO toTeamRO(SupportTeam team) {
        if (team == null) {
            return null;
        }
        return new SupportTeamRO(team.getId(), team.getName(), team.getDescription());
    }

    private TicketCommentRO toCommentRO(TicketComment comment) {
        return new TicketCommentRO(
                comment.getId(),
                toUserRO(comment.getAuthor()),
                comment.getMessage(),
                comment.getType(),
                comment.getCreatedAt()
        );
    }

    private RelatedResolvedTicketRO toRelatedResolvedTicketRO(Ticket ticket) {
        String resolution = ticket.getComments().stream()
                .filter(comment -> comment.getType() == TicketCommentType.RESOLUTION)
                .map(TicketComment::getMessage)
                .findFirst()
                .orElse("Resolution note unavailable");

        return new RelatedResolvedTicketRO(ticket.getId(), ticket.getTitle(), resolution);
    }
}
