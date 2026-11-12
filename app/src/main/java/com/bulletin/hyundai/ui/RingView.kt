package com.bulletin.hyundai.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View
import androidx.core.content.ContextCompat
import com.bulletin.hyundai.R

/**
 * "확인한 동작 N가지 중 M가지" 결과 링. 점수가 아니라 개수를 보여준다.
 * Teal = 잘한 동작, 빨강 = 놓친 것이 있는 동작. 가운데 숫자와 글자가 같은 내용을 말해 주므로
 * 색만으로 뜻이 전달되지는 않는다.
 */
class RingView(context: Context, private val compact: Boolean = false) : View(context) {

    var good = 0
        set(v) { field = v; invalidate() }
    var total = 0
        set(v) { field = v; invalidate() }

    private val density = resources.displayMetrics.density
    private val stroke = (if (compact) 7f else 16f) * density

    private fun c(id: Int) = ContextCompat.getColor(context, id)

    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = stroke; color = c(R.color.coach_line)
    }
    private val goodPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = stroke; strokeCap = Paint.Cap.ROUND; color = c(R.color.coach_good)
    }
    private val missPaint = Paint(goodPaint).apply { color = c(R.color.coach_warn) }
    private val big = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = c(R.color.coach_text); textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val small = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = c(R.color.coach_muted); textAlign = Paint.Align.CENTER
    }
    private val rect = RectF()

    override fun onDraw(canvas: Canvas) {
        val size = minOf(width, height).toFloat()
        val inset = stroke / 2 + 2 * density
        val left = (width - size) / 2
        val top = (height - size) / 2
        rect.set(left + inset, top + inset, left + size - inset, top + size - inset)
        canvas.drawOval(rect, track)

        if (total > 0) {
            // 둥근 끝이 겹치지 않도록 조각 사이에 틈을 둔다
            val gap = if (good in 1 until total) (if (compact) 14f else 10f) else 0f
            val goodSweep = 360f * good / total
            val missSweep = 360f - goodSweep
            if (good > 0) canvas.drawArc(rect, -90f + gap / 2, goodSweep - gap, false, goodPaint)
            if (good < total) canvas.drawArc(rect, -90f + goodSweep + gap / 2, missSweep - gap, false, missPaint)
        }

        val cx = width / 2f
        val cy = height / 2f
        if (compact) {
            big.textSize = size * 0.26f
            canvas.drawText("$good/$total", cx, cy + big.textSize * 0.36f, big)
        } else {
            big.textSize = size * 0.30f
            small.textSize = size * 0.10f
            canvas.drawText("$good", cx, cy + big.textSize * 0.18f, big)
            canvas.drawText("/ $total 가지 잘함", cx, cy + big.textSize * 0.18f + small.textSize * 1.6f, small)
        }
    }
}
