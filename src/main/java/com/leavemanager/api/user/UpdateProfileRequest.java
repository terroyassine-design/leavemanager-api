package com.leavemanager.api.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Le nom est obligatoire") @Size(max = 100) String fullName,
        @NotBlank(message = "L'email est obligatoire") @Email(message = "Email invalide") String email) {
}
