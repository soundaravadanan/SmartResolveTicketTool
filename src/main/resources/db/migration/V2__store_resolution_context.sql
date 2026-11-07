ALTER TABLE ticket_comment
    ADD COLUMN comment_type VARCHAR(20) NOT NULL DEFAULT 'COMMENT';

CREATE TABLE ticket_ai_context (
    ticket_id UUID NOT NULL REFERENCES support_ticket(id) ON DELETE CASCADE,
    related_ticket_id UUID NOT NULL REFERENCES support_ticket(id),
    PRIMARY KEY (ticket_id, related_ticket_id),
    CONSTRAINT chk_ticket_ai_context_not_self CHECK (ticket_id <> related_ticket_id)
);

CREATE INDEX idx_ticket_ai_context_related_ticket_id ON ticket_ai_context(related_ticket_id);
