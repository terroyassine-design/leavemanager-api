package com.leavemanager.api.leave;

import java.time.Instant;
import java.time.LocalDate;

import com.leavemanager.api.common.ApiException;
import com.leavemanager.api.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A leave request and its workflow: PENDING -> APPROVED | REJECTED | CANCELLED.
 * State transitions live here so the rules cannot be bypassed.
 */
@Entity
@Table(name = "leave_requests")
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id")
    private User employee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveType type;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private int workingDays;

    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveStatus status;

    private String managerComment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by_id")
    private User decidedBy;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant decidedAt;

    protected LeaveRequest() {
        // required by JPA
    }

    public LeaveRequest(User employee, LeaveType type, LocalDate startDate, LocalDate endDate, String reason) {
        this.employee = employee;
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
        this.workingDays = WorkingDays.between(startDate, endDate);
        this.reason = reason;
        this.status = LeaveStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public void approve(User manager, String comment) {
        decide(manager, comment, LeaveStatus.APPROVED);
        if (type == LeaveType.PAID) {
            employee.debitLeaveBalance(workingDays);
        }
    }

    public void reject(User manager, String comment) {
        decide(manager, comment, LeaveStatus.REJECTED);
    }

    public void cancel() {
        ensurePending();
        status = LeaveStatus.CANCELLED;
    }

    private void decide(User manager, String comment, LeaveStatus newStatus) {
        ensurePending();
        if (manager.getId().equals(employee.getId())) {
            throw ApiException.forbidden("Vous ne pouvez pas valider votre propre demande");
        }
        this.status = newStatus;
        this.managerComment = comment;
        this.decidedBy = manager;
        this.decidedAt = Instant.now();
    }

    private void ensurePending() {
        if (status != LeaveStatus.PENDING) {
            throw ApiException.conflict("Seule une demande en attente peut être modifiée");
        }
    }

    public Long getId() {
        return id;
    }

    public User getEmployee() {
        return employee;
    }

    public LeaveType getType() {
        return type;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public int getWorkingDays() {
        return workingDays;
    }

    public String getReason() {
        return reason;
    }

    public LeaveStatus getStatus() {
        return status;
    }

    public String getManagerComment() {
        return managerComment;
    }

    public User getDecidedBy() {
        return decidedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }
}
