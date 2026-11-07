package com.smartresolve.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "support_team")
public class SupportTeam {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    protected SupportTeam() {
    }

    public SupportTeam(UUID id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public SupportTeam(String name, String description) {
        this(UUID.randomUUID(), name, description);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
