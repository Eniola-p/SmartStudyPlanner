package com.eniola.smartstudyplanner.utils

import com.eniola.smartstudyplanner.model.CalendarDay
import java.time.LocalDate
import java.time.YearMonth

/**Generates all calendar cells for a month.*/
object CalendarUtils {

    /** Generates exactly 42 cells (6 rows × 7 columns).*/
    fun generateMonth(
        year: Int,
        month: Int
    ): List<CalendarDay> {
        val yearMonth = YearMonth.of(year, month)
        val firstDay = yearMonth.atDay(1)
        val totalDays = yearMonth.lengthOfMonth()
        val today = LocalDate.now()
        val calendarDays = mutableListOf<CalendarDay>()

        // Sunday = 0
        val firstDayOffset =
            firstDay.dayOfWeek.value % 7

        // Empty cells before day 1
        repeat(firstDayOffset) {
            calendarDays.add(
                CalendarDay(day = null)
            )
        }

        // Real dates
        for (day in 1..totalDays) {
            val currentDate =
                LocalDate.of(year, month, day)

            calendarDays.add(
                CalendarDay(
                    day = day,
                    date = currentDate,
                    isToday = currentDate == today,
                    isSelected = false
                )
            )
        }
        // Fill remaining cells to make 42
        while (calendarDays.size < 42) {
            calendarDays.add(
                CalendarDay(day = null)
            )
        }
        return calendarDays
    }
}