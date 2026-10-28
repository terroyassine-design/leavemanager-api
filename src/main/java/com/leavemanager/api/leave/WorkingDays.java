package com.leavemanager.api.leave;

import java.time.DayOfWeek;
import java.time.LocalDate;

/** Counts working days (Monday to Friday). Public holidays are not handled yet. */
public final class WorkingDays {

    private WorkingDays() {
    }

    public static int between(LocalDate start, LocalDate endInclusive) {
        return (int) start.datesUntil(endInclusive.plusDays(1))
                .filter(day -> day.getDayOfWeek() != DayOfWeek.SATURDAY && day.getDayOfWeek() != DayOfWeek.SUNDAY)
                .count();
    }
}
