package com.sait.peelin.repository;

import com.sait.peelin.model.BakeryHour;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BakeryHourRepository extends JpaRepository<BakeryHour, Integer> {
    List<BakeryHour> findByBakery_IdOrderByDayOfWeekAsc(Integer bakeryId);
}
