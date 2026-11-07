CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    role VARCHAR(40) NOT NULL
);

CREATE TABLE support_team (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(500) NOT NULL
);

CREATE TABLE support_ticket (
    id UUID PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(5000) NOT NULL,
    category VARCHAR(40) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    ai_summary VARCHAR(1000) NOT NULL,
    ai_troubleshooting VARCHAR(5000) NOT NULL,
    ai_suggested_response VARCHAR(2000) NOT NULL,
    requester_id UUID NOT NULL REFERENCES app_user(id),
    assignee_id UUID REFERENCES app_user(id),
    team_id UUID NOT NULL REFERENCES support_team(id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_support_ticket_status ON support_ticket(status);
CREATE INDEX idx_support_ticket_created_at ON support_ticket(created_at);

CREATE TABLE ticket_comment (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL REFERENCES support_ticket(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES app_user(id),
    message VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_ticket_comment_ticket_id ON ticket_comment(ticket_id);

CREATE TABLE ticket_status_history (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL REFERENCES support_ticket(id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL,
    changed_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_ticket_status_history_ticket_id ON ticket_status_history(ticket_id);
