package org.example.orderService.scheduler;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.example.orderService.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class IdempotencyCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyCleanupTask.class);

    private final ProcessedEventRepository processedEventRepository;

    public IdempotencyCleanupTask(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    @Scheduled(cron = "0 0 3 * * ?")
    @SchedulerLock(name = "IdempotencyCleanupTask_cleanup", lockAtLeastFor = "5m", lockAtMostFor = "14m")
    public void cleanupOldProcessedEvents() {
        log.info("Starting cleanup of old processed events.");
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        int deletedCount = processedEventRepository.deleteByProcessedAtBefore(threshold);
        log.info("Finished cleanup. Deleted {} events older than {}.", deletedCount, threshold);
    }
}
