package com.leavemanager.api.leave;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.leavemanager.api.common.ApiException;
import com.leavemanager.api.user.Role;
import com.leavemanager.api.user.User;

/** Pure unit tests of the workflow rules: no Spring context, no database. */
class LeaveRequestTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 2);

    private User employee;
    private User manager;

    @BeforeEach
    void setUp() {
        employee = user(1L, Role.EMPLOYEE);
        manager = user(2L, Role.MANAGER);
    }

    @Test
    void newRequestIsPendingWithComputedWorkingDays() {
        LeaveRequest leave = paidLeave(employee);

        assertThat(leave.getStatus()).isEqualTo(LeaveStatus.PENDING);
        assertThat(leave.getWorkingDays()).isEqualTo(5);
    }

    @Test
    void approvingPaidLeaveDebitsTheBalance() {
        LeaveRequest leave = paidLeave(employee);

        leave.approve(manager, "OK");

        assertThat(leave.getStatus()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(leave.getDecidedBy()).isEqualTo(manager);
        assertThat(employee.getLeaveBalance()).isEqualTo(User.DEFAULT_LEAVE_BALANCE - 5);
    }

    @Test
    void approvingUnpaidLeaveKeepsTheBalance() {
        LeaveRequest leave = new LeaveRequest(employee, LeaveType.UNPAID, MONDAY, MONDAY.plusDays(4), null);

        leave.approve(manager, null);

        assertThat(employee.getLeaveBalance()).isEqualTo(User.DEFAULT_LEAVE_BALANCE);
    }

    @Test
    void rejectingDoesNotTouchTheBalance() {
        LeaveRequest leave = paidLeave(employee);

        leave.reject(manager, "Période chargée");

        assertThat(leave.getStatus()).isEqualTo(LeaveStatus.REJECTED);
        assertThat(leave.getManagerComment()).isEqualTo("Période chargée");
        assertThat(employee.getLeaveBalance()).isEqualTo(User.DEFAULT_LEAVE_BALANCE);
    }

    @Test
    void aDecidedRequestCannotBeDecidedAgain() {
        LeaveRequest leave = paidLeave(employee);
        leave.reject(manager, null);

        assertThatThrownBy(() -> leave.approve(manager, null)).isInstanceOf(ApiException.class);
        assertThatThrownBy(leave::cancel).isInstanceOf(ApiException.class);
    }

    @Test
    void aManagerCannotApproveTheirOwnRequest() {
        LeaveRequest leave = paidLeave(manager);

        assertThatThrownBy(() -> leave.approve(manager, null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("propre demande");
    }

    @Test
    void approvalFailsWhenBalanceIsTooLow() {
        LeaveRequest longLeave = new LeaveRequest(employee, LeaveType.PAID, MONDAY, MONDAY.plusWeeks(6), null);

        assertThatThrownBy(() -> longLeave.approve(manager, null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Solde");
    }

    private static LeaveRequest paidLeave(User owner) {
        return new LeaveRequest(owner, LeaveType.PAID, MONDAY, MONDAY.plusDays(4), "Vacances");
    }

    private static User user(Long id, Role role) {
        User user = new User("User " + id, "user" + id + "@test.com", "hash", role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
