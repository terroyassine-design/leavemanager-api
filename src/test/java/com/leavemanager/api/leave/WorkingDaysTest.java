package com.leavemanager.api.leave;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class WorkingDaysTest {

    // 2026-11-02 is a Monday
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 2);

    @Test
    void countsMondayToFridayAsFiveDays() {
        assertThat(WorkingDays.between(MONDAY, MONDAY.plusDays(4))).isEqualTo(5);
    }

    @Test
    void ignoresWeekends() {
        // Monday -> next Sunday: 5 working days
        assertThat(WorkingDays.between(MONDAY, MONDAY.plusDays(6))).isEqualTo(5);
    }

    @Test
    void singleDayCountsAsOne() {
        assertThat(WorkingDays.between(MONDAY, MONDAY)).isEqualTo(1);
    }

    @Test
    void weekendOnlyIsZero() {
        LocalDate saturday = MONDAY.plusDays(5);
        assertThat(WorkingDays.between(saturday, saturday.plusDays(1))).isZero();
    }
}
