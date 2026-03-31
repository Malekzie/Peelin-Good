package com.sait.peelin.repository;

import com.sait.peelin.model.Bakery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BakeryRepository extends JpaRepository<Bakery, Integer> {

    List<Bakery> findByBakeryNameContainingIgnoreCase(String search);
}
