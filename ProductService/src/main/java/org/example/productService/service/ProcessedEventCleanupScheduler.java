package org.example.productService.service;

import org.example.productService.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ProcessedEventCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(ProcessedEventCleanupScheduler.class);
    private static final int RETENTION_DAYS = 7;

    private final ProcessedEventRepository processedEventRepository;

    public ProcessedEventCleanupScheduler(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    @Scheduled(cron = "0 30 2 * * SUN")
    public void cleanup() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(RETENTION_DAYS);
        log.info("ProcessedEvent cleanup starting — deleting rows processed before {}", threshold);
        try {
            int deleted = processedEventRepository.deleteByProcessedAtBefore(threshold);
            log.info("ProcessedEvent cleanup complete — deleted {} rows (retention={}d)", deleted, RETENTION_DAYS);
        } catch (Exception e) {
            log.error("ProcessedEvent cleanup failed", e);
        }
    }
}
