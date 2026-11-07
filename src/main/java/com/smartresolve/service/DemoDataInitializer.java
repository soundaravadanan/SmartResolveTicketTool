package com.smartresolve.service;

import com.smartresolve.domain.SupportTeam;
import com.smartresolve.domain.User;
import com.smartresolve.domain.UserRole;
import com.smartresolve.repository.SupportTeamRepository;
import com.smartresolve.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoDataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final SupportTeamRepository teamRepository;

    public DemoDataInitializer(UserRepository userRepository, SupportTeamRepository teamRepository) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        createUserIfMissing("Priya Sharma", "priya@company.com", UserRole.EMPLOYEE);
        createUserIfMissing("Arjun Mehta", "arjun@company.com", UserRole.SUPPORT_ENGINEER);
        createUserIfMissing("Nina Patel", "nina@company.com", UserRole.ADMIN);

        createTeamIfMissing("Identity & Access", "Handles login, SSO, and access issues");
        createTeamIfMissing("Application Support", "Handles app errors and production incidents");
        createTeamIfMissing("Infrastructure Ops", "Handles network and connectivity problems");
    }

    private void createUserIfMissing(String name, String email, UserRole role) {
        if (userRepository.findByEmail(email).isEmpty()) {
            userRepository.save(new User(name, email, role));
        }
    }

    private void createTeamIfMissing(String name, String description) {
        if (teamRepository.findByName(name).isEmpty()) {
            teamRepository.save(new SupportTeam(name, description));
        }
    }
}
