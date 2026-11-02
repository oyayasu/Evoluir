import com.oyayasu.evoluir.*
import java.time.LocalDate

fun main() {
    val today = LocalDate.now()
    val goal = Improvement(1, "Impaciência", "Ouvir antes de reagir", 0)
    val empty = ImprovementSummary(goal, emptyList())
    check(empty.score() == null)
    check(ProgressPalette.forScore(empty.score()) == 0xFF929992.toInt())

    val red = ImprovementSummary(goal, listOf(DailyMark(today.toString(), 0, "")))
    check(red.score() == 0f)
    check(ProgressPalette.forScore(red.score()) == 0xFFDC514F.toInt())

    val mixed = ImprovementSummary(goal, listOf(DailyMark(today.toString(), 1, ""), DailyMark(today.minusDays(1).toString(), 0, "")))
    check(mixed.score() == 0.5f)
    check(ProgressPalette.forScore(mixed.score()) == 0xFFE5BE4C.toInt())

    val old = ImprovementSummary(goal, listOf(DailyMark(today.minusDays(31).toString(), 1, "")))
    check(old.score() == null)
    val green = ImprovementSummary(goal, listOf(DailyMark(today.toString(), 1, "")))
    check(green.score() == 1f)
    check(ProgressPalette.forScore(green.score()) == 0xFF409B6B.toInt())
    println("OK: pontuação, histórico e cores")
}
