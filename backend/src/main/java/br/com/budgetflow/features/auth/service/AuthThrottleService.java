package br.com.budgetflow.features.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import br.com.budgetflow.common.exceptions.TooManyRequestsException;

@Service
public class AuthThrottleService {

    private final Map<String, AttemptWindow> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Duration window;
    private final AtomicInteger checksSinceCleanup = new AtomicInteger();

    public AuthThrottleService(
            @Value("${app.security.rate-limit.max-attempts:10}") int maxAttempts,
            @Value("${app.security.rate-limit.window-minutes:15}") long windowMinutes
    ) {
        this.maxAttempts = maxAttempts;
        this.window = Duration.ofMinutes(windowMinutes);
    }

    public void check(String key) {
        Instant now = Instant.now();
        if (checksSinceCleanup.incrementAndGet() >= 100) {
            attempts.entrySet().removeIf(entry -> entry.getValue().startedAt().plus(window).isBefore(now));
            checksSinceCleanup.set(0);
        }
        AttemptWindow current = attempts.compute(key, (ignored, existing) -> {
            if (existing == null || existing.startedAt().plus(window).isBefore(now)) {
                return new AttemptWindow(now, 1);
            }
            return new AttemptWindow(existing.startedAt(), existing.count() + 1);
        });

        if (current.count() > maxAttempts) {
            throw new TooManyRequestsException("Muitas tentativas. Tente novamente mais tarde.");
        }
    }

    public void reset(String key) {
        attempts.remove(key);
    }

    private record AttemptWindow(Instant startedAt, int count) {
    }
}
