package org.example.productService.repository;

import org.example.productService.model.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {

    @Modifying
    @Transactional
    @Query("DELETE FROM ProcessedEvent p WHERE p.processedAt < :threshold")
    int deleteByProcessedAtBefore(@Param("threshold") LocalDateTime threshold);
}
