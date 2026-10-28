package com.leavemanager.api.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.leavemanager.api.auth.AuthResponse;
import com.leavemanager.api.auth.JwtService;
import com.leavemanager.api.common.ApiException;

@Service
@Transactional
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public ProfileService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return UserResponse.from(getUser(userId));
    }

    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getUser(userId);
        String email = User.normalizeEmail(request.email());
        if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
            throw ApiException.conflict("Un compte existe déjà avec cet email");
        }
        user.updateProfile(request.fullName().trim(), email);
        return UserResponse.from(user);
    }

    /**
     * Changing the password revokes every existing token (session version + 1),
     * so a fresh token is returned to keep the current session open.
     */
    public AuthResponse changePassword(Long userId, ChangePasswordRequest request) {
        PasswordPolicy.check(request.newPassword());
        User user = getUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw ApiException.badRequest("Le mot de passe actuel est incorrect");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw ApiException.badRequest("Le nouveau mot de passe doit être différent de l'actuel");
        }
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        userRepository.flush(); // persist the new session version before issuing the token
        return new AuthResponse(jwtService.generateToken(user), UserResponse.from(user));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));
    }
}
