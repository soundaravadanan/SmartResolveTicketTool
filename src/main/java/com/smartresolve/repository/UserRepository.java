package com.smartresolve.repository;

import com.smartresolve.domain.User;
import com.smartresolve.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    List<User> findByRoleOrderByName(UserRole role);

    Optional<User> findFirstByRoleOrderByName(UserRole role);

    Optional<User> findByEmail(String email);
}
