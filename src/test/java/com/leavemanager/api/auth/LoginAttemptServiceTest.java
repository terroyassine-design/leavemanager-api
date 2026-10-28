package com.leavemanager.api.auth;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.leavemanager.api.common.ApiException;

class LoginAttemptServiceTest {

    private static final String KEY = "127.0.0.1|jean@test.com";

    private MutableClock clock;
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-11-02T09:00:00Z"));
        service = new LoginAttemptService(clock);
    }

    @Test
    void allowsLoginBelowTheLimit() {
        failTimes(LoginAttemptService.MAX_FAILURES - 1);

        assertThatCode(() -> service.checkAllowed(KEY)).doesNotThrowAnyException();
    }

    @Test
    void blocksLoginOnceTheLimitIsReached() {
        failTimes(LoginAttemptService.MAX_FAILURES);

        assertThatThrownBy(() -> service.checkAllowed(KEY)).isInstanceOf(ApiException.class);
    }

    @Test
    void unblocksAfterTheBlockDuration() {
        failTimes(LoginAttemptService.MAX_FAILURES);

        clock.advance(LoginAttemptService.BLOCK_DURATION);

        assertThatCode(() -> service.checkAllowed(KEY)).doesNotThrowAnyException();
    }

    @Test
    void otherKeysAreNotAffected() {
        failTimes(LoginAttemptService.MAX_FAILURES);

        assertThatCode(() -> service.checkAllowed("10.0.0.1|jean@test.com")).doesNotThrowAnyException();
    }

    @Test
    void successResetsTheCounter() {
        failTimes(LoginAttemptService.MAX_FAILURES - 1);
        service.recordSuccess(KEY);
        failTimes(LoginAttemptService.MAX_FAILURES - 1);

        assertThatCode(() -> service.checkAllowed(KEY)).doesNotThrowAnyException();
    }

    private void failTimes(int count) {
        for (int i = 0; i < count; i++) {
            service.recordFailure(KEY);
        }
    }

    /** A clock the test can move forward. */
    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
