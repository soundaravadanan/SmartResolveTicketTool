package com.smartresolve.api.ro;

import java.time.LocalDateTime;

public record ApiErrorRO(
        LocalDateTime timestamp,
        int status,
        String error,
        String message
) {
}
