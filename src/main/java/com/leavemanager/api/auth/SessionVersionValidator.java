package com.leavemanager.api.auth;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import com.leavemanager.api.user.UserRepository;

/**
 * Rejects tokens issued before the user's last password change (or for a deleted user).
 * Each token carries the user's session version; changing the password increments it,
 * which revokes every token issued before.
 */
public class SessionVersionValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error SESSION_REVOKED =
            new OAuth2Error("invalid_token", "La session a expiré, veuillez vous reconnecter", null);

    private final UserRepository userRepository;

    public SessionVersionValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Object version = jwt.getClaim(JwtService.SESSION_VERSION_CLAIM);
        if (!(version instanceof Number tokenVersion)) {
            return OAuth2TokenValidatorResult.failure(SESSION_REVOKED);
        }
        boolean valid;
        try {
            valid = userRepository.findById(Long.valueOf(jwt.getSubject()))
                    .map(user -> user.getSessionVersion() == tokenVersion.intValue())
                    .orElse(false);
        } catch (NumberFormatException ex) {
            valid = false;
        }
        return valid ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(SESSION_REVOKED);
    }
}
