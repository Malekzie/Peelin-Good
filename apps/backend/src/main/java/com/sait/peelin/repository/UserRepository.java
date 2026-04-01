package com.sait.peelin.repository;

import com.sait.peelin.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository("coreUserRepository")
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    Optional<User> findByUserEmail(String userEmail);
    Optional<User> findByUsernameOrUserEmail(String username, String userEmail);

    /** Same identifier for both args matches either username or email (login field). */
    Optional<User> findByUsernameIgnoreCaseOrUserEmailIgnoreCase(String username, String userEmail);

    boolean existsByUsername(String username);
    boolean existsByUserEmail(String userEmail);
}
