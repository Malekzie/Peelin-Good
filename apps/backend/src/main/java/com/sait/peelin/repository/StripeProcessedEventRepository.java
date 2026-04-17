package com.sait.peelin.repository;

import com.sait.peelin.model.StripeProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StripeProcessedEventRepository extends JpaRepository<StripeProcessedEvent, String> {
}
