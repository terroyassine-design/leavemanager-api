package com.leavemanager.api.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.leavemanager.api.common.ApiException;
import com.leavemanager.api.user.PasswordPolicy;
import com.leavemanager.api.user.Role;
import com.leavemanager.api.user.User;
import com.leavemanager.api.user.UserRepository;
import com.leavemanager.api.user.UserResponse;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttempts;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                       LoginAttemptService loginAttempts) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginAttempts = loginAttempts;
    }

    /** Self-registration always creates an EMPLOYEE; managers are created by an administrator. */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        PasswordPolicy.check(request.password());
        String email = User.normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw ApiException.conflict("Un compte existe déjà avec cet email");
        }
        User user = userRepository.save(new User(
                request.fullName().trim(), email, passwordEncoder.encode(request.password()), Role.EMPLOYEE));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request, String clientIp) {
        String email = User.normalizeEmail(request.email());
        String attemptKey = clientIp + "|" + email;
        loginAttempts.checkAllowed(attemptKey);

        User user = userRepository.findByEmail(email)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPassword()))
                .orElse(null);
        if (user == null) {
            loginAttempts.recordFailure(attemptKey);
            // same message for unknown email and wrong password: do not reveal which accounts exist
            throw ApiException.unauthorized("Email ou mot de passe incorrect");
        }
        loginAttempts.recordSuccess(attemptKey);
        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(jwtService.generateToken(user), UserResponse.from(user));
    }
}
