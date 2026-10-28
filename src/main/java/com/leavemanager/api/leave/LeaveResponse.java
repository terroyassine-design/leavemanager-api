package com.leavemanager.api.leave;

import java.time.Instant;
import java.time.LocalDate;

public record LeaveResponse(
        Long id,
        Long employeeId,
        String employeeName,
        LeaveType type,
        LocalDate startDate,
        LocalDate endDate,
        int workingDays,
        String reason,
        LeaveStatus status,
        String managerComment,
        String decidedByName,
        Instant createdAt,
        Instant decidedAt) {

    public static LeaveResponse from(LeaveRequest leave) {
        return new LeaveResponse(
                leave.getId(),
                leave.getEmployee().getId(),
                leave.getEmployee().getFullName(),
                leave.getType(),
                leave.getStartDate(),
                leave.getEndDate(),
                leave.getWorkingDays(),
                leave.getReason(),
                leave.getStatus(),
                leave.getManagerComment(),
                leave.getDecidedBy() != null ? leave.getDecidedBy().getFullName() : null,
                leave.getCreatedAt(),
                leave.getDecidedAt());
    }
}
