package org.example.productService.scheduler;

import org.example.productService.repository.ProcessedEventRepository;
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

    @Scheduled(cron = "0 0 4 * * ?")
    public void cleanupOldProcessedEvents() {
        log.info("Starting cleanup of old processed events in ProductService.");
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        try {
            int deletedCount = processedEventRepository.deleteByProcessedAtBefore(threshold);
            log.info("Finished cleanup. Deleted {} events older than {}.", deletedCount, threshold);
        } catch (Exception e) {
            log.error("Failed to cleanup old processed events.", e);
        }
    }
}
