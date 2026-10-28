package com.leavemanager.api.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.leavemanager.api.common.ApiException;

/**
 * Brute-force protection: after MAX_FAILURES wrong passwords for the same email from the same
 * IP address, login is refused (429) until BLOCK_DURATION has passed since the first failure.
 * Keyed on email + IP so an attacker cannot lock a real user out from another network.
 * In-memory: fine for a single instance; several instances would share it through Redis.
 */
@Component
public class LoginAttemptService {

    static final int MAX_FAILURES = 5;
    static final Duration BLOCK_DURATION = Duration.ofMinutes(15);
    private static final int CLEANUP_THRESHOLD = 10_000;

    private record Attempts(int failures, Instant firstFailure) {
    }

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginAttemptService(Clock clock) {
        this.clock = clock;
    }

    public void checkAllowed(String key) {
        Attempts current = attempts.get(key);
        if (current != null && current.failures() >= MAX_FAILURES && !isExpired(current)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Trop de tentatives de connexion. Réessayez dans " + BLOCK_DURATION.toMinutes() + " minutes.");
        }
    }

    public void recordFailure(String key) {
        if (attempts.size() > CLEANUP_THRESHOLD) {
            attempts.values().removeIf(this::isExpired);
        }
        attempts.compute(key, (k, current) -> current == null || isExpired(current)
                ? new Attempts(1, clock.instant())
                : new Attempts(current.failures() + 1, current.firstFailure()));
    }

    public void recordSuccess(String key) {
        attempts.remove(key);
    }

    private boolean isExpired(Attempts attempts) {
        return !clock.instant().isBefore(attempts.firstFailure().plus(BLOCK_DURATION));
    }
}
