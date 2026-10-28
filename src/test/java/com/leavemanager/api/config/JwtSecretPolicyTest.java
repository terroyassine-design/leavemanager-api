package com.leavemanager.api.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JwtSecretPolicyTest {

    private static final String STRONG_SECRET = "a-long-random-secret-generated-for-production-42";

    @Test
    void rejectsTheDevelopmentDefaultInProduction() {
        assertThatThrownBy(() -> JwtSecretPolicy.validate(JwtSecretPolicy.DEV_SECRET, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET must be set in production");
    }

    @Test
    void acceptsTheDevelopmentDefaultOutsideProduction() {
        assertThatCode(() -> JwtSecretPolicy.validate(JwtSecretPolicy.DEV_SECRET, false)).doesNotThrowAnyException();
    }

    @Test
    void rejectsSecretsShorterThan32Bytes() {
        assertThatThrownBy(() -> JwtSecretPolicy.validate("too-short", false))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void acceptsAStrongSecretInProduction() {
        assertThatCode(() -> JwtSecretPolicy.validate(STRONG_SECRET, true)).doesNotThrowAnyException();
    }
}
