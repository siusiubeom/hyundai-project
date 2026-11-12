package com.bulletin.hyundai.core

import com.bulletin.hyundai.core.Comparison.Kind as CK
import kotlin.math.roundToInt

/** 저장되는 주행 기록 한 건 */
data class DriveRecord(
    val startMs: Long,
    val routeKey: String,
    val routeName: String,
    val tallies: Map<CoachItem, ItemTally>,
) {
    /** 확인한 동작 수, 잘한 동작 수 (최근 연습 미니 링용) */
    val checkedCount: Int get() = tallies.count { it.value.opportunities > 0 }
    val goodCount: Int get() = tallies.count { it.value.opportunities > 0 && it.value.misses == 0 }
}

/** "다음 운전 때 기억할 것" 한 줄 */
data class MemoryNote(val item: CoachItem, val text: String)

data class ReportLine(val item: CoachItem, val text: String, val tally: ItemTally = ItemTally())

/** 지난번(같은 길)과 비교 한 칸 */
data class Comparison(
    val item: CoachItem,
    val kind: Kind,
    /** 놓친 비율 추이 (오래된 것 → 오늘). 기억할 것 확인 줄에서는 비어 있다. */
    val series: List<Float>,
    val text: String,
) {
    enum class Kind { MEMORY_DONE, MEMORY_PARTIAL, MEMORY_NO_CHANCE, BETTER, WORSE, SAME, NO_CHANCE }
}

data class AskQuestion(
    val moment: AskMoment,
    val question: String,
    val options: List<Phrases.AskOption>,
)

data class DriveReport(
    val routeName: String,
    /** 같은 길 몇 번째 주행인지 (이번 포함) */
    val runNumber: Int,
    val headline: String,
    val goods: List<ReportLine>,
    val improves: List<ReportLine>,
    val comparisons: List<Comparison>,
    val questions: List<AskQuestion>,
    val memory: List<MemoryNote>,
    /** 저장할 때 쓰는 최종 집계 (확인 질문 답 포함) */
    val tallies: Map<CoachItem, ItemTally>,
    /** 결과 링: 확인한 동작 수, 잘한 동작 수 */
    val checkedCount: Int,
    val goodCount: Int,
    /** 오늘의 주행 타임라인 */
    val events: List<DriveEvent>,
)

/**
 * 판정 결과 → 도착 후 리포트. 점수는 매기지 않는다.
 */
class ReportWriter(private val th: Thresholds = Thresholds()) {

    /**
     * @param answers 확인 질문 답. key = [AskMoment.id], value = "내 실수"를 골랐는가
     * @param history 같은 길의 지난 기록 (오래된 것 → 최근 순)
     * @param previousMemory 이번 출발 전에 보여준 "기억할 것"
     */
    fun build(
        result: DriveResult,
        routeName: String,
        answers: Map<Int, Boolean> = emptyMap(),
        history: List<DriveRecord> = emptyList(),
        previousMemory: List<MemoryNote> = emptyList(),
    ): DriveReport {
        val tallies = effectiveTallies(result, answers)
        val checked = tallies.filterValues { it.opportunities > 0 }

        val goods = checked.filterValues { it.misses == 0 }
            .keys.sortedByDescending { it.severity }
            .map { ReportLine(it, Phrases.good(it, checked.getValue(it)), checked.getValue(it)) }

        val missed = checked.filterValues { it.misses > 0 }
            .entries.sortedWith(compareByDescending<Map.Entry<CoachItem, ItemTally>> { it.key.severity }
                .thenByDescending { it.value.missRatio })
        val improves = missed.map { ReportLine(it.key, Phrases.miss(it.key, it.value, th), it.value) }

        val memory = missed.take(MAX_MEMORY).map { MemoryNote(it.key, Phrases.tip(it.key)) }

        val questions = result.asks.map {
            AskQuestion(
                it,
                Phrases.question(it.item, Phrases.elapsedLabel(it.atMs - result.startMs)),
                Phrases.options(it.item),
            )
        }

        return DriveReport(
            routeName = routeName,
            runNumber = history.size + 1,
            headline = Phrases.headline(checked.size, goods.size),
            goods = goods,
            improves = improves,
            comparisons = compare(tallies, history, previousMemory),
            questions = questions,
            memory = memory,
            tallies = tallies,
            checkedCount = checked.size,
            goodCount = goods.size,
            events = result.events,
        )
    }

    /** 판정 항목 + 확인 질문에 답한 것. 답하지 않은 질문은 세지 않는다. */
    fun effectiveTallies(result: DriveResult, answers: Map<Int, Boolean>): Map<CoachItem, ItemTally> {
        val out = LinkedHashMap(result.tallies)
        for (ask in result.asks) {
            val mine = answers[ask.id] ?: continue
            out[ask.item] = (out[ask.item] ?: ItemTally()) + ItemTally(1, if (mine) 1 else 0)
        }
        return out
    }

    private fun compare(
        now: Map<CoachItem, ItemTally>,
        history: List<DriveRecord>,
        previousMemory: List<MemoryNote>,
    ): List<Comparison> {
        val lines = mutableListOf<Comparison>()

        // 1) 지난번에 기억하기로 한 것
        for (note in previousMemory) {
            val t = now[note.item]
            lines += when {
                t == null || t.opportunities == 0 ->
                    Comparison(note.item, CK.MEMORY_NO_CHANCE, emptyList(), "지난번 기억할 것 '${note.item.title}': 오늘은 확인할 상황이 없었어요.")
                t.misses == 0 ->
                    Comparison(note.item, CK.MEMORY_DONE, emptyList(), "지난번 기억할 것 '${note.item.title}': 오늘 ${t.opportunities}번 모두 해냈어요!")
                else ->
                    Comparison(note.item, CK.MEMORY_PARTIAL, emptyList(), "지난번 기억할 것 '${note.item.title}': 오늘 ${t.opportunities}번 중 ${t.good}번 해냈어요.")
            }
        }

        // 2) 같은 길 지난 회차와 비율 비교
        val prev = history.lastOrNull() ?: return lines
        val recent = history.takeLast(MAX_SERIES - 1)
        val items = (prev.tallies.keys + now.keys).distinct().sortedByDescending { it.severity }
        for (item in items) {
            if (item in previousMemory.map { it.item }) continue
            val p = prev.tallies[item]
            val n = now[item]
            if (p == null || p.opportunities == 0) continue
            if (n == null || n.opportunities == 0) {
                if (p.misses > 0) lines += Comparison(item, CK.NO_CHANCE, listOf(p.missRatio), "${item.title}: 오늘은 확인할 상황이 없었어요.")
                continue
            }
            if (p.misses == 0 && n.misses == 0) continue // 계속 잘한 것은 생략

            val points = recent.mapNotNull { it.tallies[item]?.takeIf { t -> t.opportunities > 0 } } + n
            val series = points.joinToString(" → ") { pct(it) }
            val (kind, trend) = when {
                n.missRatio < p.missRatio -> CK.BETTER to "좋아졌어요"
                n.missRatio > p.missRatio -> CK.WORSE to "다시 신경 써 봐요"
                else -> CK.SAME to "지난번과 같아요"
            }
            lines += Comparison(item, kind, points.map { it.missRatio }, "${item.title}: 놓친 비율 $series, $trend")
        }
        return lines
    }

    private fun pct(t: ItemTally) = "${(t.missRatio * 100).roundToInt()}%"

    companion object {
        const val MAX_MEMORY = 3
        const val MAX_SERIES = 4
    }
}
