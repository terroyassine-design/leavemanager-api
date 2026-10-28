package com.leavemanager.api.config;

import java.nio.charset.StandardCharsets;

/**
 * Fails fast at startup when the JWT signing secret is unsafe.
 * Anyone who knows the secret can forge tokens (including MANAGER ones), so production
 * must never run with the development default that is committed in application.properties.
 */
public final class JwtSecretPolicy {

    /** Must match the default value of app.jwt.secret in application.properties. */
    static final String DEV_SECRET = "local-dev-secret-not-for-production-1234567890";

    /** HS256 requires a key of at least 256 bits. */
    static final int MIN_BYTES = 32;

    private JwtSecretPolicy() {
    }

    public static void validate(String secret, boolean production) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_BYTES) {
            throw new IllegalStateException("app.jwt.secret (JWT_SECRET) must be at least " + MIN_BYTES + " bytes long");
        }
        if (production && DEV_SECRET.equals(secret)) {
            throw new IllegalStateException(
                    "JWT_SECRET must be set in production: the default development secret is public");
        }
    }
}
