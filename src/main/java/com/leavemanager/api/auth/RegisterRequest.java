package com.leavemanager.api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Le nom est obligatoire") @Size(max = 100) String fullName,
        @NotBlank(message = "L'email est obligatoire") @Email(message = "Email invalide") String email,
        @Size(min = 8, max = 72, message = "Le mot de passe doit contenir entre 8 et 72 caractères") String password) {
}
