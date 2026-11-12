package com.bulletin.hyundai.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View
import androidx.core.content.ContextCompat
import com.bulletin.hyundai.R
import com.bulletin.hyundai.core.ReportLine
import kotlin.math.cos
import kotlin.math.sin

/** Vehicle-style instruments. All values are observed counts, never a synthetic score. */
class InstrumentPanel(context: Context, private val good: Int, private val total: Int,
                      private val review: Int) : View(context) {
    private val d = resources.displayMetrics.density
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private fun color(id: Int) = ContextCompat.getColor(context, id)
    private fun text(c: Canvas, value: String, x: Float, y: Float, size: Float,
                     ink: Int = R.color.coach_text, bold: Boolean = false) {
        p.style = Paint.Style.FILL; p.color = color(ink); p.textSize = size
        p.textAlign = Paint.Align.CENTER
        p.typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        c.drawText(value, x, y, p)
    }
    init {
        contentDescription = "주행 결과: 확인한 동작 ${total}가지 중 ${good}가지 잘함, 보완 ${total-good}가지, 확인 질문 ${review}개"
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }
    private fun dial(c: Canvas, cx: Float, cy: Float, r: Float, value: Int, max: Int,
                     label: String, major: Boolean, ink: Int) {
        val bounds = RectF(cx-r, cy-r, cx+r, cy+r)
        p.style = Paint.Style.STROKE; p.strokeWidth = r * .055f
        p.strokeCap = Paint.Cap.ROUND; p.color = color(R.color.coach_line)
        c.drawArc(bounds, 140f, 260f, false, p)
        val fraction = if (max > 0) (value.toFloat()/max).coerceIn(0f,1f) else 0f
        if (fraction > 0) {
            p.color = color(ink)
            c.drawArc(bounds, 140f, 260f*fraction, false, p)
        }
        p.strokeCap = Paint.Cap.BUTT
        for (i in 0..40) {
            val a = Math.toRadians((140+i*6.5).toDouble())
            val outer = r * .90f
            val inner = r * (if (i%5==0) .79f else .85f)
            p.color = color(if (i/40f <= fraction && max > 0) ink else R.color.coach_line)
            p.strokeWidth = if(i%5==0) 2.2f*d else d
            c.drawLine(cx+cos(a).toFloat()*inner,cy+sin(a).toFloat()*inner,
                cx+cos(a).toFloat()*outer,cy+sin(a).toFloat()*outer,p)
        }
        if (major) {
            text(c,"0",cx-r*.65f,cy+r*.53f,r*.075f,R.color.coach_muted)
            text(c,"$max",cx+r*.65f,cy+r*.53f,r*.075f,R.color.coach_muted)
            val a = Math.toRadians((140+260*fraction).toDouble())
            p.style = Paint.Style.STROKE; p.color=color(ink);p.strokeWidth=3*d
            c.drawLine(cx,cy,cx+cos(a).toFloat()*r*.65f,cy+sin(a).toFloat()*r*.65f,p)
            p.style=Paint.Style.FILL;p.color=color(R.color.coach_surface)
            c.drawCircle(cx,cy,r*.39f,p)
            p.color=color(ink); c.drawCircle(cx,cy,4*d,p)
        }
        text(c,if(max==0) "—" else "$value",cx,cy-r*.1f,r*(if(major) .36f else .42f),ink,true)
        text(c,label,cx,cy+r*.20f,r*.11f,R.color.coach_muted)
        text(c,if(major) "확인한 ${max}가지 중" else "${max}가지 중",cx,cy+r*.66f,r*.095f,R.color.coach_muted)
    }
    override fun onDraw(c: Canvas) {
        val w=width.toFloat(); val h=height.toFloat()
        if(w > 700*d) {
            val r=minOf(h*.43f,w*.23f)
            dial(c,w*.5f,h*.48f,r,good,total,"잘한 동작",true,R.color.coach_primary)
            dial(c,w*.16f,h*.51f,r*.57f,total-good,total,"다음에 보완",false,R.color.coach_warn)
            dial(c,w*.84f,h*.51f,r*.57f,total,total,"확인한 동작",false,R.color.coach_primary)
        } else {
            val r=minOf(w*.43f,h*.34f)
            dial(c,w*.5f,r+16*d,r,good,total,"잘한 동작",true,R.color.coach_primary)
            dial(c,w*.26f,h*.79f,r*.48f,total-good,total,"다음에 보완",false,R.color.coach_warn)
            dial(c,w*.74f,h*.79f,r*.48f,total,total,"확인한 동작",false,R.color.coach_primary)
        }
    }
}

/** One continuous plot rather than a grid of result cards. */
class BehaviorGraph(context: Context, private val lines: List<ReportLine>) : View(context) {
    private val d=resources.displayMetrics.density
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    private fun ink(id:Int)=ContextCompat.getColor(context,id)
    init {
        contentDescription = lines.joinToString(". ") { "${it.item.title}: ${it.text}" }
        importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_YES
    }
    override fun onDraw(c:Canvas) {
        val w=width.toFloat(); val compact=w<700*d
        val left=if(compact) 0f else 245*d
        val right=w-72*d
        val row=if(compact) 80*d else 61*d
        lines.forEachIndexed { i,l ->
            val y= i*row + 24*d
            p.style=Paint.Style.FILL;p.typeface=Typeface.create("sans-serif",Typeface.NORMAL)
            p.textAlign=Paint.Align.LEFT;p.textSize=15*d;p.color=ink(R.color.coach_text)
            c.drawText(l.item.title,0f,y,p)
            val by=if(compact)y+22*d else y-5*d
            val track=RectF(left,by-4*d,right,by+4*d)
            p.color=ink(R.color.coach_line);c.drawRoundRect(track,4*d,4*d,p)
            val t=l.tally
            val success=(t.opportunities-t.misses).coerceAtLeast(0)
            val ratio=if(t.opportunities>0)success.toFloat()/t.opportunities else 0f
            p.color=ink(R.color.coach_primary)
            if(ratio>0)c.drawRoundRect(RectF(left,track.top,left+(right-left)*ratio,track.bottom),4*d,4*d,p)
            if(t.misses>0){
                p.color=ink(R.color.coach_warn)
                c.drawRoundRect(RectF(left+(right-left)*ratio+2*d,track.top,right,track.bottom),4*d,4*d,p)
            }
            p.color=ink(R.color.coach_text);p.textAlign=Paint.Align.RIGHT;p.textSize=15*d
            c.drawText("$success/${t.opportunities}",w,by+5*d,p)
            p.color=ink(R.color.coach_line);p.strokeWidth=d
            c.drawLine(0f,(i+1)*row,w,(i+1)*row,p)
        }
    }
}

class TrendGraph(context: Context, private val values: List<Float>) : View(context) {
    private val d=resources.displayMetrics.density
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    init { contentDescription="놓친 비율 추이: "+values.joinToString(", ") { "${(it*100).toInt()}%" } }
    override fun onDraw(c:Canvas) {
        if(values.size<2)return
        val bottom=height-24*d; val top=16*d
        val left=28*d; val right=width-28*d
        p.strokeWidth=d;p.color=ContextCompat.getColor(context,R.color.coach_line)
        for(i in 0..2){ val y=top+(bottom-top)*i/2;c.drawLine(left,y,right,y,p) }
        val points=values.mapIndexed { i,v -> left+(right-left)*i/(values.size-1) to bottom-(bottom-top)*v.coerceIn(0f,1f) }
        p.color=ContextCompat.getColor(context,R.color.coach_primary);p.strokeWidth=3*d
        points.zipWithNext().forEach { (a,b)->c.drawLine(a.first,a.second,b.first,b.second,p) }
        points.forEachIndexed { i,(x,y)->
            p.style=Paint.Style.FILL;c.drawCircle(x,y,5*d,p)
            p.textSize=12*d;p.textAlign=Paint.Align.CENTER
            c.drawText("${(values[i]*100).toInt()}%",x,y-10*d,p)
        }
    }
}
