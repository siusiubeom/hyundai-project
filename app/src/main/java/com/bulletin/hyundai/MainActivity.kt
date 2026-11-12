package com.bulletin.hyundai

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bulletin.hyundai.core.CoachSession
import com.bulletin.hyundai.core.DemoScripts
import com.bulletin.hyundai.core.DriveRecord
import com.bulletin.hyundai.core.DriveReport
import com.bulletin.hyundai.core.DriveResult
import com.bulletin.hyundai.core.DriveScript
import com.bulletin.hyundai.core.MemoryNote
import com.bulletin.hyundai.core.ReportWriter
import com.bulletin.hyundai.core.Thresholds
import com.bulletin.hyundai.core.VehicleSignalSource
import com.bulletin.hyundai.core.VehicleSnapshot
import com.bulletin.hyundai.ui.Screens
import com.bulletin.hyundai.ui.Ui
import com.bulletin.hyundai.data.DriveRepository
import com.bulletin.hyundai.databinding.ActivityMainBinding
import com.bulletin.hyundai.signal.PleosVehicleSignalSource
import com.bulletin.hyundai.signal.ScriptSignalSource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), CoachSession.Listener {

    private lateinit var b: ActivityMainBinding
    private lateinit var repo: DriveRepository

    private val thresholds = Thresholds()
    private val writer = ReportWriter(thresholds)
    private val session = CoachSession(thresholds, this)

    private var source: VehicleSignalSource? = null
    private var pulse: android.animation.ObjectAnimator? = null

    private fun startPulse() {
        pulse?.cancel()
        pulse = android.animation.ObjectAnimator.ofFloat(b.recBadge, "alpha", 1f, 0.55f, 1f).apply {
            duration = 2400
            repeatCount = android.animation.ValueAnimator.INFINITE
            start()
        }
    }

    /** 이번 주행의 길 이름. 내비 연동 전까지는 각본이 정하고, 없으면 날짜로 저장. */
    private var routeKey = ""
    private var routeName = ""

    /** 이번 출발 전에 보여준 "기억할 것" (도착 후 비교에 사용) */
    private var shownMemory: List<MemoryNote> = emptyList()

    private var pending: DriveResult? = null
    private val answers = mutableMapOf<Int, Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        delegate.localNightMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        repo = DriveRepository(this)
        val ui = Ui(this)
        b.recBadge.addView(ui.badge(R.drawable.ic_steering, ui.color(R.color.coach_accent), 160, ring = true).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(ui.dp(160), ui.dp(160), android.view.Gravity.CENTER)
        })

        b.btnRun1.setOnClickListener { play(DemoScripts.RUN_1) }
        b.btnRun2.setOnClickListener { play(DemoScripts.RUN_2) }
        b.btnSignals.setOnClickListener {
            b.signalPanel.visibility = if (b.signalPanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        b.btnReset.setOnClickListener {
            repo.clear()
            showHome()
        }
        showHome()

        val missing = PleosVehicleSignalSource.PERMISSIONS.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) requestPermissions(missing.toTypedArray(), REQ_PERMISSIONS)
        else startLive()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_PERMISSIONS) startLive() // 일부 거부돼도 받을 수 있는 신호는 받는다
    }

    override fun onDestroy() {
        pulse?.cancel()
        source?.stop()
        super.onDestroy()
    }

    // ───────────────────────── 신호 ─────────────────────────

    /** 차량(또는 플레오스 에뮬레이터) 신호 받기. 차량 OS 가 아니면 시연 각본만 쓴다. */
    private fun startLive() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) return
        if (source != null) return
        // 플레오스 에뮬레이터는 안전띠·주차 브레이크 값을 콘솔에서 바꿀 수 없어 항상 "안 함"으로 읽힌다
        val ignored = if (android.os.Build.MODEL.contains("emulator", ignoreCase = true)) setOf("seatbelt", "parkingBrake") else emptySet()
        source = runCatching { PleosVehicleSignalSource(this, ignored = ignored).also { it.start(::onSnapshot) } }
            .onFailure { android.util.Log.w("Coach", "차량 신호 시작 실패", it) }
            .getOrNull()
    }

    private fun play(script: DriveScript) {
        source?.stop()
        routeKey = script.routeKey
        routeName = script.routeName
        source = ScriptSignalSource(script, onEnd = ::onSourceEnded).also { it.start(::onSnapshot) }
    }

    private fun onSnapshot(s: VehicleSnapshot) {
        session.onSnapshot(s)
        if (b.signalPanel.visibility == View.VISIBLE) b.signalPanel.text = describe(s)
    }

    private fun onSourceEnded() {
        source = null
        session.last?.let { if (session.isDriving) session.abort(it.timeMs) }
        startLive()
    }

    // ─────────────────────── 주행 흐름 ───────────────────────

    override fun onDriveStarted() {
        shownMemory = repo.memory()
        if (routeKey.isEmpty()) {
            val now = Date()
            routeKey = "date-" + SimpleDateFormat("yyyyMMddHHmm", Locale.KOREA).format(now)
            routeName = SimpleDateFormat("M월 d일 주행", Locale.KOREA).format(now)
        }
        show(b.drivingScreen)
        b.demoBar.visibility = View.GONE
    }

    override fun onDriveFinished(result: DriveResult) {
        pending = result
        answers.clear()
        renderReport()
        show(b.reportScreen)
        b.reportScreen.scrollTo(0, 0)
    }

    private fun saveAndClose() {
        val result = pending ?: return
        val report = buildReport(result)
        repo.save(DriveRecord(result.startMs, routeKey, routeName, report.tallies))
        repo.saveMemory(report.memory)
        pending = null
        routeKey = ""
        routeName = ""
        showHome()
    }

    private fun buildReport(result: DriveResult): DriveReport =
        writer.build(result, routeName, answers, repo.history(routeKey), shownMemory)

    // ───────────────────────── 화면 ─────────────────────────

    private fun show(screen: View) {
        listOf(b.homeScreen, b.drivingScreen, b.reportScreen).forEach {
            it.visibility = if (it == screen) View.VISIBLE else View.GONE
        }
    }

    private fun showHome() {
        Screens(Ui(this))
            .home(b.homeContent, repo.memory(), repo.records())
        show(b.homeScreen)
        b.demoBar.visibility = View.VISIBLE
    }

    private fun renderReport() {
        val result = pending ?: return
        val r = buildReport(result)
        val c = b.reportContent
        val keepScroll = b.reportScreen.scrollY
        Screens(Ui(this)).report(
            c, r, result.startMs, answers,
            onAnswer = { id, mine -> answers[id] = mine; renderReport() },
            onSave = ::saveAndClose,
        )

        b.reportScreen.post { b.reportScreen.scrollTo(0, keepScroll) }
    }

    // ───────────────────────── 뷰 도우미 ─────────────────────────

    private fun describe(s: VehicleSnapshot): String {
        fun onOff(v: Boolean?) = when (v) { true -> "ON"; false -> "OFF"; null -> "?" }
        return buildString {
            appendLine("기어        ${s.gear}")
            appendLine("속도        ${"%.1f".format(s.speedKmh)} km/h")
            appendLine("핸들        ${"%.0f".format(s.steeringDeg)}°")
            appendLine("깜빡이      ${s.turnSignal}")
            appendLine("브레이크    ${onOff(s.brakePressed)}")
            appendLine("주차브레이크 ${onOff(s.parkingBrakeOn)}")
            appendLine("문 열림     ${onOff(s.doorOpen)}")
            append("안전띠      ${onOff(s.seatbeltOn)}")
            val live = source as? PleosVehicleSignalSource ?: run {
                append("\n\n[시연 각본]")
                return@buildString
            }
            append("\n\n[차량 신호] 마지막 수신")
            val now = System.currentTimeMillis()
            listOf("gear", "speed", "steering", "turnSignal", "brake", "parkingBrake", "door", "seatbelt").forEach { k ->
                val t = live.lastReceived[k]
                append("\n  ${k.padEnd(12)} ${if (t == null) "없음" else "${(now - t) / 1000}초 전"}")
            }
        }
    }

    private companion object {
        const val REQ_PERMISSIONS = 1
    }
}
