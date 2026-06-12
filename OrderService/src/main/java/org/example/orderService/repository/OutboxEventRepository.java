package org.example.orderService.repository;

import org.example.orderService.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxEvent.EventStatus status);

    @Query("SELECT e FROM OutboxEvent e WHERE e.status = 'PROCESSING' AND e.createdAt < :threshold")
    List<OutboxEvent> findStuckProcessingEvents(@Param("threshold") LocalDateTime threshold);

    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.status IN :statuses AND e.createdAt < :cutoff")
    int deleteOldTerminalEvents(
            @Param("statuses") List<OutboxEvent.EventStatus> statuses,
            @Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT e.status, COUNT(e) FROM OutboxEvent e GROUP BY e.status")
    List<Object[]> countGroupedByStatus();

    @Query("SELECT COUNT(e) FROM OutboxEvent e WHERE e.status = 'PROCESSING' AND e.createdAt < :threshold")
    long countStuckProcessingEvents(@Param("threshold") LocalDateTime threshold);

    long countByStatus(OutboxEvent.EventStatus status);
}
