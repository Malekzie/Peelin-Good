package com.sait.peelin.repository;

import com.sait.peelin.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Integer> {
    Optional<Tag> findByTagNameIgnoreCase(String tagName);
}
