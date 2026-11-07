package com.smartresolve.service;

import java.util.UUID;

public record ResolvedTicketContext(UUID ticketId, String title, String resolution) {
}
