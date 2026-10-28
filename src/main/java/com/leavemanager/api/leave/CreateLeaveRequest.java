package com.leavemanager.api.leave;

import java.time.LocalDate;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateLeaveRequest(
        @NotNull(message = "Le type est obligatoire") LeaveType type,
        @NotNull(message = "La date de début est obligatoire")
        @FutureOrPresent(message = "La date de début ne peut pas être dans le passé") LocalDate startDate,
        @NotNull(message = "La date de fin est obligatoire") LocalDate endDate,
        @Size(max = 500) String reason) {
}
