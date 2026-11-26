package com.lafarguem.pumptracker.data

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

enum class StatsRange(val label: String, val title: String) {
    WEEK("Week", "Last 7 days"),
    MONTH("Month", "Last 30 days"),
    YEAR("Year", "Last 12 months"),
}

/** One bar of the chart: [label] goes under the bar (may be blank), [title] describes it in full. */
data class Bar(val label: String, val title: String, val value: Int)

data class BestDay(val date: LocalDate, val total: Int)

data class Stats(
    val bars: List<Bar>,
    val periodTotal: Int,
    /** Average per calendar day of the period, rest days included. */
    val dailyAverage: Double,
    val allTimeTotal: Int,
    val bestDay: BestDay?,
    /** Consecutive days with at least one rep, ending today (or yesterday if today is still empty). */
    val currentStreak: Int,
)

/** [totals] maps an epoch day to the reps done that day. */
fun computeStats(
    totals: Map<Long, Int>,
    today: LocalDate,
    range: StatsRange,
    locale: Locale = Locale.getDefault(),
): Stats {
    fun on(date: LocalDate) = totals[date.toEpochDay()] ?: 0

    val dayTitle = DateTimeFormatter.ofPattern("EEE d MMM", locale)
    val bars: List<Bar>
    val periodDays: Long
    when (range) {
        StatsRange.WEEK -> {
            periodDays = 7
            bars = (6L downTo 0L).map { back ->
                val date = today.minusDays(back)
                Bar(date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale), date.format(dayTitle), on(date))
            }
        }

        StatsRange.MONTH -> {
            periodDays = 30
            bars = (29L downTo 0L).map { back ->
                val date = today.minusDays(back)
                // Label every 5th bar, anchored on today, so the axis stays readable.
                val label = if (back % 5 == 0L) date.dayOfMonth.toString() else ""
                Bar(label, date.format(dayTitle), on(date))
            }
        }

        StatsRange.YEAR -> {
            val monthTitle = DateTimeFormatter.ofPattern("MMMM yyyy", locale)
            val current = YearMonth.from(today)
            val first = current.minusMonths(11)
            periodDays = today.toEpochDay() - first.atDay(1).toEpochDay() + 1
            val byMonth = totals.entries.groupingBy { YearMonth.from(LocalDate.ofEpochDay(it.key)) }
                .fold(0) { sum, entry -> sum + entry.value }
            bars = (0L..11L).map { i ->
                val month = first.plusMonths(i)
                Bar(
                    month.month.getDisplayName(TextStyle.NARROW, locale),
                    month.format(monthTitle),
                    byMonth[month] ?: 0,
                )
            }
        }
    }

    val periodTotal = bars.sumOf { it.value }

    var cursor = if (on(today) > 0) today else today.minusDays(1)
    var streak = 0
    while (on(cursor) > 0) {
        streak++
        cursor = cursor.minusDays(1)
    }

    return Stats(
        bars = bars,
        periodTotal = periodTotal,
        dailyAverage = periodTotal.toDouble() / periodDays,
        allTimeTotal = totals.values.sum(),
        bestDay = totals.entries.filter { it.value > 0 }.maxByOrNull { it.value }
            ?.let { BestDay(LocalDate.ofEpochDay(it.key), it.value) },
        currentStreak = streak,
    )
}
