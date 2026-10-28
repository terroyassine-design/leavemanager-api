package com.leavemanager.api.user;

import java.nio.charset.StandardCharsets;

import com.leavemanager.api.common.ApiException;

/**
 * BCrypt only uses the first 72 BYTES of a password. Bean Validation counts characters,
 * and an accented character takes 2 bytes in UTF-8, so the byte length is checked here.
 */
public final class PasswordPolicy {

    static final int BCRYPT_MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    public static void check(String rawPassword) {
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw ApiException.badRequest(
                    "Le mot de passe est trop long (72 octets maximum, les caractères accentués comptent double)");
        }
    }
}
