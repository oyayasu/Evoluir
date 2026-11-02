package com.oyayasu.evoluir

import java.time.LocalDate

data class Improvement(
    val id: Long,
    val name: String,
    val description: String,
    val createdAt: Long,
    val symbol: String = "●"
)

data class DailyMark(val date: String, val outcome: Int, val note: String)

data class ImprovementSummary(
    val improvement: Improvement,
    val marks: List<DailyMark>
) {
    fun markOn(day: String): DailyMark? = marks.firstOrNull { it.date == day }
    fun recentMarks(days: Int = 30, today: LocalDate = LocalDate.now()): List<DailyMark> {
        val start = today.minusDays((days - 1).toLong()).toString()
        val end = today.toString()
        return marks.filter { it.date >= start && it.date <= end && it.outcome in 0..1 }
    }
    fun score(days: Int = 30): Float? {
        val relevant = recentMarks(days)
        return if (relevant.isEmpty()) null else relevant.count { it.outcome == 1 }.toFloat() / relevant.size
    }
}

/** Red -> orange -> yellow -> pale green -> green; no data = neutral gray. */
object ProgressPalette {
    private val stops = intArrayOf(0xFFDC514F.toInt(), 0xFFEA8B39.toInt(), 0xFFE5BE4C.toInt(), 0xFF98BF68.toInt(), 0xFF409B6B.toInt())
    val UNRECORDED: Int = 0xFFCDD2CD.toInt()

    fun forScore(score: Float?): Int {
        if (score == null) return 0xFF929992.toInt()
        val position = score.coerceIn(0f, 1f) * 4
        val lower = position.toInt().coerceAtMost(3)
        val fraction = position - lower
        val a = stops[lower]
        val b = stops[lower + 1]
        fun channel(shift: Int): Int {
            val first = (a shr shift) and 0xFF
            val second = (b shr shift) and 0xFF
            return (first + ((second - first) * fraction)).toInt().coerceIn(0, 255)
        }
        return (0xFF shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }

    fun forDay(mark: DailyMark?): Int = when (mark?.outcome) {
        0 -> stops[0]
        1 -> stops[4]
        else -> UNRECORDED
    }
}
