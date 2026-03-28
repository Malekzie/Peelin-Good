package com.sait.peelin.repository;

import com.sait.peelin.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    Optional<User> findByUserEmail(String userEmail);
    Optional<User> findByUsernameOrUserEmail(String username, String userEmail);

    boolean existsByUsername(String username);
    boolean existsByUserEmail(String userEmail);
}
