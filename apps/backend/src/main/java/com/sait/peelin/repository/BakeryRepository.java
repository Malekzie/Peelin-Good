package com.sait.peelin.repository;

import com.sait.peelin.model.Bakery;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BakeryRepository extends JpaRepository<Bakery, Integer> {

    @EntityGraph(attributePaths = "address")
    List<Bakery> findAll();

    @EntityGraph(attributePaths = "address")
    List<Bakery> findByBakeryNameContainingIgnoreCase(String search);

    @EntityGraph(attributePaths = "address")
    Optional<Bakery> findById(Integer id);
}
