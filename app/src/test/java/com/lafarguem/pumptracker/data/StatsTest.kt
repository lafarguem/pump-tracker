package com.lafarguem.pumptracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class StatsTest {
    private val today = LocalDate.of(2026, 11, 26)
    private fun day(back: Long) = today.minusDays(back).toEpochDay()
    private fun stats(totals: Map<Long, Int>, range: StatsRange) = computeStats(totals, today, range, Locale.US)

    @Test
    fun emptyHistory() {
        val s = stats(emptyMap(), StatsRange.WEEK)
        assertEquals(7, s.bars.size)
        assertEquals(0, s.periodTotal)
        assertEquals(0, s.currentStreak)
        assertNull(s.bestDay)
    }

    @Test
    fun weekOnlyCountsLastSevenDays() {
        val s = stats(mapOf(day(0) to 50, day(6) to 20, day(7) to 999), StatsRange.WEEK)
        assertEquals(70, s.periodTotal)
        assertEquals(10.0, s.dailyAverage, 1e-9)
        assertEquals(20, s.bars.first().value)
        assertEquals(50, s.bars.last().value)
        assertEquals("Thu", s.bars.last().label)
        assertEquals(1069, s.allTimeTotal)
        assertEquals(BestDay(today.minusDays(7), 999), s.bestDay)
    }

    @Test
    fun monthHasThirtyBarsEndingToday() {
        val s = stats(mapOf(day(29) to 5, day(30) to 7), StatsRange.MONTH)
        assertEquals(30, s.bars.size)
        assertEquals(5, s.periodTotal)
        assertEquals("26", s.bars.last().label)
    }

    @Test
    fun yearGroupsByCalendarMonth() {
        val totals = mapOf(
            LocalDate.of(2026, 11, 1).toEpochDay() to 10,
            LocalDate.of(2026, 11, 26).toEpochDay() to 5,
            LocalDate.of(2025, 12, 31).toEpochDay() to 3,
            LocalDate.of(2025, 11, 30).toEpochDay() to 100,
        )
        val s = stats(totals, StatsRange.YEAR)
        assertEquals(12, s.bars.size)
        assertEquals(15, s.bars.last().value)
        assertEquals("November 2026", s.bars.last().title)
        assertEquals(3, s.bars.first().value)
        assertEquals(18, s.periodTotal)
    }

    @Test
    fun streakSurvivesAnEmptyToday() {
        assertEquals(3, stats(mapOf(day(1) to 1, day(2) to 1, day(3) to 1, day(5) to 1), StatsRange.WEEK).currentStreak)
        assertEquals(2, stats(mapOf(day(0) to 1, day(1) to 1), StatsRange.WEEK).currentStreak)
        assertEquals(0, stats(mapOf(day(2) to 1), StatsRange.WEEK).currentStreak)
    }
}
