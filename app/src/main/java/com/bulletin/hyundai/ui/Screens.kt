package com.bulletin.hyundai.ui

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import com.bulletin.hyundai.R
import com.bulletin.hyundai.core.Comparison
import com.bulletin.hyundai.core.DriveEvent
import com.bulletin.hyundai.core.DriveRecord
import com.bulletin.hyundai.core.DriveReport
import com.bulletin.hyundai.core.MemoryNote
import com.bulletin.hyundai.core.ReportLine
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 출발 전 / 도착 후 화면을 그림 위주로 조립한다. */
class Screens(private val ui: Ui) {

    private val ctx = ui.ctx
    private val good get() = ui.color(R.color.coach_good)
    private val warn get() = ui.color(R.color.coach_warn)
    private val accent get() = ui.color(R.color.coach_accent)
    private val muted get() = ui.color(R.color.coach_muted)
    private val wide get() = ctx.resources.configuration.screenWidthDp >= 900
    private fun columns(preferred: Int) = if (wide) preferred else 1

    private fun photo(res: Int, height: Int) = android.widget.ImageView(ctx).apply {
        setImageResource(res)
        scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
        layoutParams = LinearLayout.LayoutParams(Ui.MATCH, ui.dp(height))
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private fun editorial(res: Int, eyebrow: String, title: String, detail: String) = ui.card(0).apply {
        clipToOutline = true
        addView(photo(res, if (wide) 140 else 170))
        addView(ui.column(
            ui.text(eyebrow, 12f, R.color.coach_accent, bold = true),
            ui.text(title, 21f, bold = true, top = 10),
            ui.text(detail, 16f, R.color.coach_muted, top = 8),
        ).apply { setPadding(ui.dp(22), ui.dp(22), ui.dp(22), ui.dp(24)) })
    }

    // ───────────────────────── 출발 전 ─────────────────────────

    fun home(c: LinearLayout, memory: List<MemoryNote>, recent: List<DriveRecord>) {
        c.removeAllViews()
        c.addView(ui.row().apply {
            addView(ui.text("DRIVE  /  COACH", 17f, bold = true).apply { letterSpacing = 0.16f; layoutParams = LinearLayout.LayoutParams(0, Ui.WRAP, 1f) })
            addView(ui.pill("출발 전 · P", R.drawable.ic_gear_p, accent, 13f))
        })
        c.addView(ui.card(0).apply {
            (layoutParams as LinearLayout.LayoutParams).topMargin = ui.dp(28)
            background = ui.rounded(ui.color(R.color.coach_surface), 24, ui.color(R.color.coach_line), 1)
            clipToOutline = true
            addView(photo(R.drawable.drive_hero, if (wide) 300 else 220))
            addView(ui.column(
                ui.text("YOUR NEXT CHAPTER", 12f, R.color.coach_primary, bold = true).apply { letterSpacing = 0.18f },
                ui.text("좋은 운전은,\n작은 여유에서 시작돼요.", if (wide) 36f else 28f, R.color.coach_text, bold = true, top = 14),
                ui.text("매번의 연습을 기억하고, 다음 주행을 함께 준비해요.", 17f, R.color.coach_muted, top = 12),
            ).apply { setPadding(ui.dp(30), ui.dp(26), ui.dp(30), ui.dp(30)) })
        })

        if (memory.isEmpty()) {
            c.addView(ui.text("첫 주행을 위한 준비", 26f, bold = true, top = 32))
            c.addView(ui.card(24).apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = ui.dp(28)
                gravity = Gravity.CENTER_HORIZONTAL
                addView(ui.badge(R.drawable.ic_steering, accent, 64))
                addView(ui.text("기어를 D로 바꾸면 기록을 시작해요", 22f, bold = true, top = 18).apply { gravity = Gravity.CENTER })
                addView(ui.text("주행 중에는 화면을 띄우지 않아요. 도착해서 P로 바꾸면 정리해 드릴게요.", 18f, R.color.coach_muted, top = 8).apply { gravity = Gravity.CENTER })
            })
        } else {
            c.addView(ui.text("오늘은 이것만 기억해요", 28f, bold = true, top = 32))
            c.addView(ui.text("지난 운전에서 정한 ${memory.size}가지예요. 위험한 것부터 놓았어요.", 19f, R.color.coach_muted, top = 6))
            c.addView(memoryCards(memory, top = 28))
            c.addView(ui.row(top = 20).apply {
                addView(ui.icon(R.drawable.ic_gear_p, muted, 22).apply { (layoutParams as LinearLayout.LayoutParams).marginEnd = ui.dp(8) })
                addView(ui.text("기어를 D로 바꾸면 기록을 시작해요. 주행 중에는 화면을 띄우지 않아요.", 17f, R.color.coach_muted).apply { layoutParams = ui.lp(Ui.WRAP) })
            })
        }

        if (recent.isNotEmpty()) {
            c.addView(ui.section("최근 연습", R.drawable.ic_flag))
            c.addView(ui.grid(recent.takeLast(4).reversed().map(::recentCard), columns(2)))
        }
        c.addView(ui.section("여유로운 운전을 위한 습관", R.drawable.ic_bulb, "출발 전, 잠깐의 준비가 더 편안한 주행을 만들어요."))
        c.addView(ui.grid(listOf(
            editorial(R.drawable.drive_coast, "01 / BEFORE YOU GO", "출발은 천천히", "안전띠와 주변을 확인하고, 부드럽게 출발해요."),
            editorial(R.drawable.drive_city, "02 / FIND YOUR RHYTHM", "도심에서도 여유롭게", "방향지시등은 미리, 앞차와의 거리는 넉넉하게."),
            editorial(R.drawable.drive_forest, "03 / A BETTER TOMORROW", "조금씩, 더 나은 운전", "도착 후 주행을 돌아보고 다음에 기억할 것을 정해요."),
        ), columns(3)))
        c.addView(ui.text("DRIVE WITH CONFIDENCE  ·  나의 속도로, 더 편안하게", 12f, R.color.coach_muted, top = 28))
    }

    private fun recentCard(r: DriveRecord) = ui.card(20).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(RingView(ctx, compact = true).apply {
            good = r.goodCount; total = r.checkedCount
            layoutParams = LinearLayout.LayoutParams(ui.dp(72), ui.dp(72)).apply { marginEnd = ui.dp(16) }
        })
        addView(ui.column(
            ui.text(r.routeName, 19f, bold = true),
            ui.text(SimpleDateFormat("M월 d일 a h:mm", Locale.KOREA).format(Date(r.startMs)), 15f, R.color.coach_muted, top = 2),
        ).apply { layoutParams = LinearLayout.LayoutParams(0, Ui.WRAP, 1f) })
    }

    private fun memoryCards(memory: List<MemoryNote>, top: Number) =
        ui.grid(memory.mapIndexed { i, n -> memoryCard(i + 1, n) }, columns(3), top = top)

    private fun memoryCard(n: Int, note: MemoryNote) = ui.card(28).apply {
        addView(ui.row().apply {
            addView(FrameLayout(ctx).apply {
                background = ui.oval(accent)
                layoutParams = LinearLayout.LayoutParams(ui.dp(44), ui.dp(44)).apply { marginEnd = ui.dp(16) }
                addView(ui.text("$n", 22f, R.color.white, bold = true).apply {
                    gravity = Gravity.CENTER
                    layoutParams = FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH)
                })
            })
            addView(ui.text(note.item.title, 18f, R.color.coach_muted, bold = true).apply {
                layoutParams = LinearLayout.LayoutParams(0, Ui.WRAP, 1f)
            })
        })
        addView(ui.badge(Visuals.icon(note.item), accent, 96).apply {
            (layoutParams as LinearLayout.LayoutParams).topMargin = ui.dp(24)
        })
        addView(ui.text(note.text, 24f, bold = true, top = 20))
    }

    // ───────────────────────── 도착 후 ─────────────────────────

    fun report(
        c: LinearLayout,
        r: DriveReport,
        startMs: Long,
        answers: Map<Int, Boolean>,
        onAnswer: (Int, Boolean) -> Unit,
        onSave: () -> Unit,
    ) {
        c.removeAllViews()
        c.addView(ui.row().apply {
            addView(ui.text("DRIVE / INSIGHTS", 16f, R.color.coach_primary, bold = true).apply {
                letterSpacing = 0.16f; layoutParams = LinearLayout.LayoutParams(0, Ui.WRAP, 1f)
            })
            addView(ui.pill("도착 · P", R.drawable.ic_flag, accent, 13f))
        })
        c.addView(ui.text("${r.routeName}  /  ${r.runNumber}번째 주행", 17f, R.color.coach_muted, top = 22))
        c.addView(hero(r))
        val lines = r.improves + r.goods
        if (lines.isNotEmpty()) {
            c.addView(ui.section("동작별 주행 밸런스", R.drawable.ic_check, "TEAL 잘한 횟수  ·  RED 놓친 횟수  /  각 동작의 전체 기회 중"))
            c.addView(BehaviorGraph(ctx, lines).apply {
                layoutParams = LinearLayout.LayoutParams(Ui.MATCH, ui.dp(lines.size * if(wide) 61 else 80)).apply { topMargin=ui.dp(24) }
            })
            r.improves.forEach { line ->
                c.addView(ui.text("${line.item.title}  ·  ${line.text}", 17f, R.color.coach_warn, top = 16))
            }
        }
        if (r.events.isNotEmpty()) {
            c.addView(ui.section("주행 타임라인", R.drawable.ic_start))
            c.addView(timeline(r.events, startMs))
        }

        if (r.comparisons.isNotEmpty()) {
            c.addView(ui.section("지난번과 비교", R.drawable.ic_trend_up, "같은 길(${r.routeName})의 주행끼리, 놓친 비율로 비교해요."))
            r.comparisons.forEach { c.addView(compareTile(it)) }
        }

        if (r.questions.isNotEmpty()) {
            c.addView(ui.section("확인 질문", R.drawable.ic_question, "채점하지 않은 순간이에요. 옆에 탄 분과 함께 골라 주세요."))
            r.questions.forEach { q ->
                c.addView(questionCard(q.moment.item.let(Visuals::icon), q.question, q.options, answers[q.moment.id]) { mine ->
                    onAnswer(q.moment.id, mine)
                })
            }
        }

        c.addView(ui.section("다음 운전 때 기억할 것", R.drawable.ic_bulb, "다음 출발 전에 그대로 다시 보여 드릴게요."))
        if (r.memory.isEmpty()) {
            c.addView(ui.card(28).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                (layoutParams as LinearLayout.LayoutParams).topMargin = ui.dp(16)
                addView(ui.badge(R.drawable.ic_check, good, 72).apply { (layoutParams as LinearLayout.LayoutParams).marginEnd = ui.dp(20) })
                addView(ui.text("이번엔 따로 기억할 것이 없어요. 잘했어요!", 24f, bold = true))
            })
        } else {
            r.memory.forEachIndexed { i, note ->
                c.addView(ui.text("0${i+1}  /  ${note.item.title}", 16f, R.color.coach_primary, bold = true, top = 24))
                c.addView(ui.text(note.text, 22f, bold = true, top = 8))
            }
        }

        c.addView(MaterialButton(ctx).apply {
            text = "저장하고 마치기"
            textSize = 22f
            cornerRadius = ui.dp(20)
            minHeight = ui.dp(72)
            setPadding(ui.dp(40), 0, ui.dp(40), 0)
            setOnClickListener { onSave() }
            layoutParams = LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP).apply { topMargin = ui.dp(40) }
        })
    }

    private fun hero(r: DriveReport) = ui.column().apply {
        addView(InstrumentPanel(ctx, r.goodCount, r.checkedCount, r.questions.size).apply {
            layoutParams = LinearLayout.LayoutParams(Ui.MATCH, ui.dp(if (wide) 360 else 480))
        })
        addView(ui.text(r.headline, if (wide) 28f else 23f, bold = true).apply { gravity = Gravity.CENTER })
        addView(ui.text(heroLine(r), 17f, R.color.coach_muted, top = 10).apply { gravity = Gravity.CENTER })
    }

    private fun heroLine(r: DriveReport): String {
        val better = r.comparisons.firstOrNull {
            it.kind == Comparison.Kind.BETTER || it.kind == Comparison.Kind.MEMORY_DONE
        }
        return when {
            better != null -> "지난번보다 '${better.item.title}'이(가) 좋아졌어요."
            r.improves.isEmpty() && r.checkedCount > 0 -> "확인한 동작을 모두 잘했어요."
            r.improves.isNotEmpty() -> "아쉬운 ${r.improves.size}가지는 다음 출발 전에 다시 알려 드릴게요."
            else -> "점수 대신, 몇 번 중 몇 번인지로 보여 드려요."
        }
    }

    // ── 타임라인 ──

    private fun timeline(events: List<DriveEvent>, startMs: Long): View {
        val strip = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(ui.dp(8), ui.dp(8), ui.dp(8), ui.dp(8))
        }
        events.forEachIndexed { i, e ->
            if (i > 0) strip.addView(View(ctx).apply {
                setBackgroundColor(ui.color(R.color.coach_line))
                layoutParams = LinearLayout.LayoutParams(ui.dp(56), ui.dp(4)).apply { topMargin = ui.dp(44) }
            })
            strip.addView(timelineNode(e, startMs))
        }
        return ui.column().apply {
            (layoutParams as LinearLayout.LayoutParams).topMargin = ui.dp(16)
            addView(HorizontalScrollView(ctx).apply {
                isHorizontalScrollBarEnabled = false
                addView(strip)
            })
        }
    }

    private fun timelineNode(e: DriveEvent, startMs: Long): View {
        val tint = when (e.ok) { true -> good; false -> warn; null -> accent }
        val status = when (e.ok) { true -> R.drawable.ic_check; false -> R.drawable.ic_alert; null -> R.drawable.ic_question }
        val sec = ((e.atMs - startMs) / 1000).coerceAtLeast(0)
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ui.dp(120), Ui.WRAP)
            addView(FrameLayout(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(ui.dp(92), ui.dp(92))
                addView(ui.badge(Visuals.icon(e), tint, 88, ring = true).apply {
                    layoutParams = FrameLayout.LayoutParams(ui.dp(88), ui.dp(88))
                })
                addView(FrameLayout(ctx).apply {
                    background = ui.oval(tint, ui.color(R.color.coach_surface), 3)
                    layoutParams = FrameLayout.LayoutParams(ui.dp(32), ui.dp(32), Gravity.BOTTOM or Gravity.END)
                    addView(ui.icon(status, ui.color(R.color.white), 32).apply {
                        layoutParams = FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH)
                        setPadding(ui.dp(5), ui.dp(5), ui.dp(5), ui.dp(5))
                    })
                })
            })
            addView(ui.text(Visuals.label(e), 18f, bold = true, top = 10).apply { gravity = Gravity.CENTER })
            addView(ui.text("%d:%02d".format(sec / 60, sec % 60), 14f, R.color.coach_muted).apply { gravity = Gravity.CENTER })
        }
    }

    // ── 동작 타일 ──

    // ── 비교 타일 ──

    private fun compareTile(cmp: Comparison): View {
        val (tint, iconRes, label) = when (cmp.kind) {
            Comparison.Kind.BETTER -> Triple(good, R.drawable.ic_trend_down, "좋아졌어요")
            Comparison.Kind.WORSE -> Triple(warn, R.drawable.ic_trend_up, "다시 신경 써요")
            Comparison.Kind.SAME -> Triple(muted, R.drawable.ic_trend_flat, "지난번과 같아요")
            Comparison.Kind.NO_CHANCE -> Triple(muted, R.drawable.ic_trend_flat, "오늘은 상황 없음")
            Comparison.Kind.MEMORY_DONE -> Triple(good, R.drawable.ic_check, "약속 지켰어요")
            Comparison.Kind.MEMORY_PARTIAL -> Triple(warn, R.drawable.ic_alert, "조금 더 해 봐요")
            Comparison.Kind.MEMORY_NO_CHANCE -> Triple(muted, R.drawable.ic_trend_flat, "오늘은 상황 없음")
        }
        val isMemory = cmp.kind.name.startsWith("MEMORY")
        return ui.column(top = 24).apply {
            addView(ui.row().apply {
                addView(ui.badge(Visuals.icon(cmp.item), tint, 56).apply {
                    (layoutParams as LinearLayout.LayoutParams).marginEnd = ui.dp(14)
                })
                addView(ui.column(
                    ui.text(if (isMemory) "지난번 기억할 것" else "놓친 비율", 14f, R.color.coach_muted),
                    ui.text(cmp.item.title, 19f, bold = true),
                ).apply { layoutParams = LinearLayout.LayoutParams(0, Ui.WRAP, 1f) })
            })
            if (cmp.series.size >= 2) {
                addView(TrendGraph(ctx, cmp.series).apply {
                    layoutParams = LinearLayout.LayoutParams(Ui.MATCH, ui.dp(120)).apply { topMargin=ui.dp(16) }
                })
            } else if (isMemory) {
                addView(ui.text(cmp.text.substringAfter(": "), 20f, bold = true, top = 16))
            }
            addView(ui.pill(label, iconRes, tint).apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = ui.dp(14)
            })
        }
    }

    // ── 확인 질문 ──

    private fun questionCard(
        iconRes: Int,
        question: String,
        options: List<com.bulletin.hyundai.core.Phrases.AskOption>,
        answered: Boolean?,
        onPick: (Boolean) -> Unit,
    ) = ui.column(top = 24).apply {
        orientation = LinearLayout.HORIZONTAL
        (layoutParams as LinearLayout.LayoutParams).topMargin = ui.dp(16)
        addView(ui.badge(iconRes, accent, 88, ring = true).apply {
            (layoutParams as LinearLayout.LayoutParams).marginEnd = ui.dp(24)
        })
        addView(ui.column(
            ui.text(question, 23f, bold = true),
            ui.row(top = 16).apply {
                options.forEach { opt ->
                    val selected = answered == opt.myMistake
                    val btn = if (selected) MaterialButton(ctx)
                    else MaterialButton(ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle)
                    addView(btn.apply {
                        text = opt.text
                        textSize = 18f
                        isAllCaps = false
                        cornerRadius = ui.dp(16)
                        minHeight = ui.dp(60)
                        setPadding(ui.dp(24), 0, ui.dp(24), 0)
                        if (selected) setIconResource(R.drawable.ic_check)
                        setOnClickListener { onPick(opt.myMistake) }
                        layoutParams = LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP).apply { marginEnd = ui.dp(12) }
                    })
                }
            },
            ui.text(
                when (answered) {
                    null -> "아직 답하지 않았어요. 답하지 않으면 세지 않아요."
                    true -> "기억할 것에 담아 둘게요."
                    false -> "상황에 맞는 대응이었네요. 감점하지 않아요."
                },
                16f, R.color.coach_muted, top = 10,
            ),
        ).apply { layoutParams = LinearLayout.LayoutParams(0, Ui.WRAP, 1f) })
    }
}
