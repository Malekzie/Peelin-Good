package com.sait.peelin.repository.user;

import com.sait.peelin.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);

    Optional<User> findByUserEmailIs(String userEmail);

    boolean existsByUsername(String username);

//    Optional<User> findByProviderAndProviderId
}
