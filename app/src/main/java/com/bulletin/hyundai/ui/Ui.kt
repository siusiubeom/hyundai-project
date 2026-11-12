package com.bulletin.hyundai.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.bulletin.hyundai.R

/** 화면 조립용 작은 부품들. 차량 화면(어두운 배경, 큰 글씨) 기준. */
class Ui(val ctx: Context) {

    fun dp(v: Number) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), ctx.resources.displayMetrics,
    ).toInt()

    fun color(id: Int) = ContextCompat.getColor(ctx, id)

    fun alpha(colorInt: Int, a: Int) = ColorUtils.setAlphaComponent(colorInt, a)

    fun rounded(fill: Int, radiusDp: Number = 20, stroke: Int? = null, strokeDp: Number = 1) =
        GradientDrawable().apply {
            cornerRadius = dp(radiusDp).toFloat()
            setColor(fill)
            if (stroke != null) setStroke(dp(strokeDp), stroke)
        }

    fun oval(fill: Int, stroke: Int? = null, strokeDp: Number = 3) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(fill)
        if (stroke != null) setStroke(dp(strokeDp), stroke)
    }

    fun lp(w: Int = MATCH, h: Int = WRAP, top: Number = 0, end: Number = 0, weight: Float = 0f) =
        LinearLayout.LayoutParams(w, h, weight).apply {
            topMargin = dp(top)
            marginEnd = dp(end)
        }

    fun text(s: CharSequence, sp: Float, colorId: Int = R.color.coach_text, bold: Boolean = false, top: Number = 0) =
        TextView(ctx).apply {
            text = s
            textSize = sp
            setTextColor(color(colorId))
            setLineSpacing(0f, 1.18f)
            includeFontPadding = false
            typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
            layoutParams = lp(top = top)
        }

    fun column(vararg children: View, top: Number = 0) = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = lp(top = top)
        children.forEach(::addView)
    }

    fun row(top: Number = 0, gravity: Int = Gravity.CENTER_VERTICAL) = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        this.gravity = gravity
        layoutParams = lp(top = top)
    }

    fun icon(res: Int, tint: Int, sizeDp: Number) = ImageView(ctx).apply {
        setImageResource(res)
        imageTintList = ColorStateList.valueOf(tint)
        layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
    }

    /** 옅은 원 위의 아이콘. 상태 색을 입힌다. */
    fun badge(res: Int, tint: Int, sizeDp: Number, ring: Boolean = false): FrameLayout =
        FrameLayout(ctx).apply {
            background = oval(alpha(tint, 22), if (ring) tint else null)
            layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
            val pad = (sizeDp.toFloat() * 0.22f)
            addView(ImageView(ctx).apply {
                setImageResource(res)
                imageTintList = ColorStateList.valueOf(tint)
                layoutParams = FrameLayout.LayoutParams(MATCH, MATCH)
                setPadding(dp(pad), dp(pad), dp(pad), dp(pad))
            })
        }

    /** 아이콘 + 글자 알약. 상태는 항상 글자로도 말한다. */
    fun pill(label: String, iconRes: Int?, tint: Int, sp: Float = 15f) = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = rounded(alpha(tint, 18), 100)
        setPadding(dp(12), dp(6), dp(14), dp(6))
        layoutParams = LinearLayout.LayoutParams(WRAP, WRAP)
        if (iconRes != null) addView(icon(iconRes, tint, sp * 1.3f).apply {
            (layoutParams as LinearLayout.LayoutParams).marginEnd = dp(6)
        })
        addView(TextView(ctx).apply {
            text = label
            textSize = sp
            setTextColor(tint)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        })
    }

    fun card(padDp: Number = 24, stroke: Int? = null) = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(color(R.color.coach_surface), 22, stroke ?: color(R.color.coach_line), 1)
        setPadding(dp(padDp), dp(padDp), dp(padDp), dp(padDp))
        layoutParams = lp()
    }

    fun section(title: String, iconRes: Int, hint: String? = null) = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = lp(top = 40)
        addView(row().apply {
            addView(icon(iconRes, color(R.color.coach_accent), 24).apply {
                (layoutParams as LinearLayout.LayoutParams).marginEnd = dp(10)
            })
            addView(TextView(ctx).apply {
                text = title
                textSize = 22f
                setTextColor(color(R.color.coach_text))
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            })
        })
        if (hint != null) addView(text(hint, 16f, R.color.coach_muted, top = 4))
    }

    /** 같은 높이의 칸으로 n 열 격자를 만든다. */
    fun grid(items: List<View>, cols: Int, gapDp: Number = 16, top: Number = 16): LinearLayout =
        LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = lp(top = top)
            items.chunked(cols).forEachIndexed { r, chunk ->
                val rowView = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    isBaselineAligned = false
                    layoutParams = lp(top = if (r == 0) 0 else gapDp)
                }
                for (i in 0 until cols) {
                    val v = chunk.getOrNull(i) ?: View(ctx)
                    v.layoutParams = LinearLayout.LayoutParams(0, MATCH, 1f).apply {
                        if (i < cols - 1) marginEnd = dp(gapDp)
                    }
                    rowView.addView(v)
                }
                addView(rowView)
            }
        }

    companion object {
        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    }
}
