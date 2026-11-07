package com.smartresolve.api.ro;

import com.smartresolve.domain.UserRole;

import java.util.UUID;

public record UserRO(UUID id, String name, String email, UserRole role) {
}
