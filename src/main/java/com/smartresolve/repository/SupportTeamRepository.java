package com.smartresolve.repository;

import com.smartresolve.domain.SupportTeam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupportTeamRepository extends JpaRepository<SupportTeam, UUID> {

    List<SupportTeam> findAllByOrderByName();

    Optional<SupportTeam> findByName(String name);
}
