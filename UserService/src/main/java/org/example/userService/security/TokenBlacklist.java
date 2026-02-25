package org.example.userService.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenBlacklist {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklist.class);

    private final ConcurrentHashMap<String, Long> blacklist = new ConcurrentHashMap<>();

    public void add(String jti, long expiryEpochSecond) {
        blacklist.put(jti, expiryEpochSecond);
        log.debug("Blacklisted JTI {} (expires at epoch {})", jti, expiryEpochSecond);
    }

    public boolean isBlacklisted(String jti) {
        Long expiry = blacklist.get(jti);
        if (expiry == null) return false;
        return Instant.now().getEpochSecond() < expiry;
    }

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void evictExpired() {
        long now = Instant.now().getEpochSecond();
        int before = blacklist.size();
        blacklist.entrySet().removeIf(entry -> entry.getValue() <= now);
        int removed = before - blacklist.size();
        if (removed > 0) {
            log.debug("TokenBlacklist eviction: removed {} expired entries, {} remaining", removed, blacklist.size());
        }
    }
}
