package com.sait.peelin.repository;

import com.sait.peelin.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    boolean existsByUsernameIgnoreCaseAndUserIdNot(String username, UUID userId);

    boolean existsByUserEmailIgnoreCaseAndUserIdNot(String userEmail, UUID userId);

    @Modifying
    @Query(value = """
            UPDATE "user"
            SET profile_photo_path = :photoPath,
                photo_approval_pending = :pending
            WHERE user_id = :userId
            """, nativeQuery = true)
    int updateProfilePhotoState(
            @Param("userId") UUID userId,
            @Param("photoPath") String photoPath,
            @Param("pending") boolean pending
    );
}
