package com.leavemanager.api.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Le mot de passe actuel est obligatoire") String currentPassword,
        @Size(min = 8, max = 72, message = "Le mot de passe doit contenir entre 8 et 72 caractères") String newPassword) {
}
