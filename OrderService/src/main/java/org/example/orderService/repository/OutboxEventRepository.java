package org.example.orderService.repository;

import org.example.orderService.model.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxEvent.EventStatus status, Pageable pageable);

    @Modifying
    @Query("UPDATE OutboxEvent e SET e.status = :newStatus WHERE e.id IN :ids")
    int updateStatusForIds(@Param("newStatus") OutboxEvent.EventStatus newStatus, @Param("ids") Collection<Long> ids);

    @Modifying
    @Query("UPDATE OutboxEvent e SET e.status = :newStatus, e.processedAt = :processedAt WHERE e.id IN :ids")
    int updateStatusAndProcessedAtForIds(@Param("newStatus") OutboxEvent.EventStatus newStatus, @Param("processedAt") LocalDateTime processedAt, @Param("ids") Collection<Long> ids);

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
