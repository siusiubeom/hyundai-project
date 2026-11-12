package com.bulletin.hyundai.core

import kotlin.math.abs
import kotlin.math.max

/** 한 항목의 결과: 그 상황이 몇 번 있었고, 그중 몇 번 놓쳤는가 */
data class ItemTally(val opportunities: Int = 0, val misses: Int = 0) {
    val good: Int get() = opportunities - misses
    val missRatio: Float get() = if (opportunities == 0) 0f else misses.toFloat() / opportunities

    operator fun plus(o: ItemTally) =
        ItemTally(opportunities + o.opportunities, misses + o.misses)
}

/** 채점하지 않고 사람에게 물어볼 순간 */
data class AskMoment(
    val id: Int,
    val item: CoachItem,
    val atMs: Long,
    val speedKmh: Float,
)

/** 타임라인에 찍히는 주행 중 장면 */
data class DriveEvent(
    val atMs: Long,
    val kind: Kind,
    /** true = 이 장면의 판정 모두 잘함, false = 놓친 것 있음, null = 묻는 순간 */
    val ok: Boolean?,
    val missed: List<CoachItem> = emptyList(),
    val ask: AskMoment? = null,
) {
    enum class Kind { START, TURN_LEFT, TURN_RIGHT, STOP, ASK, END }
}

/** 한 번의 주행(D ~ P) 결과 */
data class DriveResult(
    val startMs: Long,
    val endMs: Long,
    val distanceM: Float,
    val tallies: Map<CoachItem, ItemTally>,
    val asks: List<AskMoment>,
    val events: List<DriveEvent> = emptyList(),
)

/**
 * 주행 한 번을 처음부터 끝까지 지켜보며 판정한다. 안드로이드 의존성 없음(단위 테스트 가능).
 *
 * 사용법: 기어가 P 에서 벗어난 첫 스냅샷부터 [onSnapshot] 으로 계속 넣는다.
 * 기어가 P 로 돌아오면 마무리 구간(주차 브레이크 확인)을 거쳐 [isFinished] 가 true 가 되고
 * [result] 를 꺼낼 수 있다.
 */
class DriveEvaluator(private val th: Thresholds = Thresholds()) {

    private val tallies = linkedMapOf<CoachItem, ItemTally>()
    private val asks = mutableListOf<AskMoment>()

    // 타임라인: 놓친 항목을 순서대로 쌓아 두고, 장면마다 구간을 잘라 붙인다
    private val missLog = mutableListOf<CoachItem>()
    private val events = mutableListOf<DriveEvent>()
    private var lastTurnEvent = -1
    private var stopMarker = 0
    private var closingMarker = 0

    private fun missesSince(marker: Int) = missLog.subList(marker, missLog.size).toList()

    private fun addEvent(atMs: Long, kind: DriveEvent.Kind, marker: Int): Int {
        val missed = missesSince(marker)
        events += DriveEvent(atMs, kind, missed.isEmpty(), missed)
        return events.lastIndex
    }

    private var prev: VehicleSnapshot? = null
    private var startMs = -1L
    private var distance = 0f

    var isFinished = false
        private set
    var result: DriveResult? = null
        private set

    // 출발·차문
    private var hasMoved = false
    private var doorKnown = false
    private var doorOpenWhileMoving = false
    private var doorOpenNotInP = false

    // 깜빡이
    private var signalOnDistance = 0f

    // 회전
    private class Turn(
        val dir: TurnSignal, val signaled: Boolean, val leadMeters: Float, var maxSpeed: Float,
        val startMs: Long, val marker: Int,
    ) {
        var dropped = false
    }
    private var turn: Turn? = null

    private class CancelWatch(val dir: TurnSignal, val startDistance: Float)
    private var cancelWatch: CancelWatch? = null

    // 정차
    private var stopped = false
    private var stopStartMs = 0L
    private var stopSawD = false
    private var stopNoBrakeMs = 0f
    private var stopNoBrakeMiss = false
    private var stopNeutralJudged = false
    private var coastNeutral = false

    // 핸들 움직임 (급핸들 / 차선 변경)
    private class Excursion(val sign: Int, val startMs: Long, val speedAtStart: Float) {
        var peak = 0f
        var maxRate = 0f
        var signalSeen = false
        var becameTurn = false
    }
    private var excursion: Excursion? = null
    private class LaneHalf(val sign: Int, val startMs: Long, val endMs: Long, val signalSeen: Boolean)
    private var lastLaneHalf: LaneHalf? = null

    // 가감속
    private val speedHistory = ArrayDeque<Pair<Long, Float>>()
    private var hardBrakeArmed = true
    private var moveStartMs = -1L
    private var hardStartAsked = false

    // 마무리 (P 이후)
    private var closingStartMs = -1L
    private var closingParkingBrakeSeen = false
    private var closingParkingBrakeKnown = false

    fun onSnapshot(s: VehicleSnapshot) {
        if (isFinished) return
        val p = prev
        if (p == null) {
            onFirst(s)
            prev = s
            return
        }
        val dt = (s.timeMs - p.timeMs) / 1000f
        if (dt <= 0f) return

        distance += p.speedKmh / 3.6f * dt
        val accel = pushSpeedAndGetAccel(s)
        if (s.doorOpen != null) doorKnown = true

        if (s.turnSignal != p.turnSignal && s.turnSignal.isDirectional()) {
            signalOnDistance = distance
        }

        if (closingStartMs >= 0) {
            if (s.gear == Gear.P) {
                handleClosing(p, s)
                prev = s
                return
            }
            closingStartMs = -1 // P 에서 다시 출발: 같은 주행으로 이어간다
        } else if (s.gear == Gear.P) {
            startClosing(s)
            prev = s
            return
        }

        trackStartAndDoor(p, s)
        trackStop(s, dt)
        trackTurn(s)
        trackCancel(s)
        trackExcursion(p, s, dt)
        trackAccel(s, accel)
        prev = s
    }

    /** 신호가 끊기는 등 정상 종료가 안 될 때 강제로 마감한다. */
    fun forceFinish(atMs: Long) {
        if (isFinished) return
        if (closingStartMs < 0) {
            wrapUpDriving()
            closingMarker = missLog.size
        }
        finishInternal(atMs)
    }

    // ───────────────────────── 출발 ─────────────────────────

    private fun onFirst(s: VehicleSnapshot) {
        startMs = s.timeMs
        speedHistory.addLast(s.timeMs to s.speedKmh)
        if (s.doorOpen != null) doorKnown = true
        // 안전띠: 기어를 P 에서 빼는 순간 매고 있었는가
        s.seatbeltOn?.let { record(CoachItem.SEATBELT, miss = !it) }
        if (s.turnSignal.isDirectional()) signalOnDistance = 0f
    }

    private fun trackStartAndDoor(p: VehicleSnapshot, s: VehicleSnapshot) {
        if (!hasMoved && s.speedKmh >= th.movingKmh) {
            hasMoved = true
            moveStartMs = p.timeMs
            // 주차 브레이크가 걸린 채 출발 (3)
            s.parkingBrakeOn?.let { record(CoachItem.PARKING_BRAKE_RELEASE, miss = it) }
            addEvent(p.timeMs, DriveEvent.Kind.START, 0)  // 안전띠 + 주차 브레이크
        }
        // 주행 중 문 열림 (1)
        if (s.doorOpen == true && s.speedKmh >= 1f) doorOpenWhileMoving = true
        // P 가 아닌데 정차 중 문을 엶 (46)
        if (s.doorOpen == true && p.doorOpen != true && s.speedKmh < th.movingKmh) doorOpenNotInP = true
    }

    // ───────────────────────── 정차 ─────────────────────────

    private fun trackStop(s: VehicleSnapshot, dt: Float) {
        if (!stopped && s.gear == Gear.N && s.speedKmh >= th.coastNeutralMinKmh) coastNeutral = true

        if (!hasMoved) return
        val nowStopped = if (stopped) s.speedKmh < th.movingKmh else s.speedKmh < th.stoppedKmh
        if (!stopped && nowStopped) {
            stopped = true
            stopMarker = missLog.size
            // 멈추기 전에 미리 N 으로 뒀는가 (18)
            record(CoachItem.COAST_NEUTRAL, miss = coastNeutral)
            coastNeutral = false
            stopStartMs = s.timeMs
            stopSawD = false
            stopNoBrakeMs = 0f
            stopNoBrakeMiss = false
            stopNeutralJudged = false
        } else if (stopped && !nowStopped) {
            endStop()
            moveStartMs = s.timeMs
            hardStartAsked = false
            return
        }
        if (!stopped) return

        // 정차 중(D) 브레이크를 떼고 있음 (20)
        if (s.gear == Gear.D) {
            stopSawD = true
            if (!s.brakePressed) {
                stopNoBrakeMs += dt * 1000f
                if (stopNoBrakeMs > th.stopNoBrakeSec * 1000f) stopNoBrakeMiss = true
            } else {
                stopNoBrakeMs = 0f
            }
        }
        // 10초 이상 정차 중 N 이 아님 (4)
        if (!stopNeutralJudged && s.timeMs - stopStartMs >= (th.stopNeutralSec * 1000).toLong()) {
            stopNeutralJudged = true
            record(CoachItem.STOP_NEUTRAL, miss = s.gear != Gear.N)
        }
    }

    private fun endStop() {
        if (!stopped) return
        if (stopSawD) record(CoachItem.STOP_BRAKE, miss = stopNoBrakeMiss)
        addEvent(stopStartMs, DriveEvent.Kind.STOP, stopMarker)
        stopped = false
    }

    // ───────────────────────── 회전 ─────────────────────────

    private fun trackTurn(s: VehicleSnapshot) {
        val absDeg = abs(s.steeringDeg)
        val dir = if (s.steeringDeg > 0) TurnSignal.LEFT else TurnSignal.RIGHT
        val t = turn
        if (t == null) {
            if (s.gear == Gear.D && absDeg >= th.turnStartDeg && s.speedKmh >= th.turnMinSpeedKmh) {
                // 직전 회전의 깜빡이 끄기 확인이 남아 있으면 정리
                cancelWatch?.let { record(CoachItem.TURN_SIGNAL_CANCEL, miss = false) }
                cancelWatch = null
                val signaled = s.turnSignal == dir
                turn = Turn(dir, signaled, distance - signalOnDistance, s.speedKmh, s.timeMs, missLog.size)
                excursion?.becameTurn = true
            }
            return
        }
        t.maxSpeed = max(t.maxSpeed, s.speedKmh)
        if (t.signaled && s.turnSignal != t.dir) t.dropped = true

        if (absDeg < th.turnEndDeg) {
            record(CoachItem.TURN_SIGNAL_ON, miss = !t.signaled)                   // 30
            record(CoachItem.TURN_SLOW, miss = t.maxSpeed > th.turnSlowMaxKmh)     // 37
            if (t.signaled) {
                record(CoachItem.TURN_SIGNAL_EARLY, miss = t.leadMeters < th.signalLeadMeters) // 31
                record(CoachItem.TURN_SIGNAL_HOLD, miss = t.dropped)                            // 32
                if (!t.dropped) cancelWatch = CancelWatch(t.dir, distance)
            }
            val kind = if (t.dir == TurnSignal.LEFT) DriveEvent.Kind.TURN_LEFT else DriveEvent.Kind.TURN_RIGHT
            lastTurnEvent = addEvent(t.startMs, kind, t.marker)
            turn = null
        }
    }

    private fun trackCancel(s: VehicleSnapshot) {
        val w = cancelWatch ?: return
        if (s.turnSignal != w.dir) {
            record(CoachItem.TURN_SIGNAL_CANCEL, miss = false)
            cancelWatch = null
        } else if (distance - w.startDistance >= th.signalCancelMeters) {
            record(CoachItem.TURN_SIGNAL_CANCEL, miss = true)                     // 33
            cancelWatch = null
            // 깜빡이 끄기는 회전이 끝난 뒤 판정되므로, 앞서 찍은 회전 장면에 덧붙인다
            if (lastTurnEvent >= 0) {
                val e = events[lastTurnEvent]
                events[lastTurnEvent] = e.copy(ok = false, missed = e.missed + CoachItem.TURN_SIGNAL_CANCEL)
            }
        }
    }

    // ─────────────────── 핸들 움직임 (묻는 것) ───────────────────

    private fun trackExcursion(p: VehicleSnapshot, s: VehicleSnapshot, dt: Float) {
        if (s.gear != Gear.D) {
            excursion = null
            return
        }
        val absDeg = abs(s.steeringDeg)
        val sign = if (s.steeringDeg > 0) 1 else -1
        val rate = abs(s.steeringDeg - p.steeringDeg) / dt
        val e = excursion
        if (e == null) {
            if (absDeg >= th.steerExcursionDeg) excursion = newExcursion(sign, s, rate)
            return
        }
        e.peak = max(e.peak, absDeg)
        e.maxRate = max(e.maxRate, rate)
        if (s.turnSignal.isDirectional()) e.signalSeen = true
        if (turn != null) e.becameTurn = true

        if (absDeg < th.steerExcursionDeg || sign != e.sign) {
            finishExcursion(e, s)
            excursion = if (absDeg >= th.steerExcursionDeg) newExcursion(sign, s, rate) else null
        }
    }

    private fun newExcursion(sign: Int, s: VehicleSnapshot, rate: Float) =
        Excursion(sign, s.timeMs, s.speedKmh).apply {
            peak = abs(s.steeringDeg)
            maxRate = rate
            signalSeen = s.turnSignal.isDirectional()
            becameTurn = turn != null
        }

    private fun finishExcursion(e: Excursion, s: VehicleSnapshot) {
        if (e.becameTurn) {
            lastLaneHalf = null
            return
        }
        val durSec = (s.timeMs - e.startMs) / 1000f
        if (e.peak >= th.quickSteerMinDeg && e.maxRate >= th.quickSteerDegPerSec &&
            durSec <= th.quickSteerMaxSec && e.speedAtStart >= th.quickSteerMinKmh
        ) {
            ask(CoachItem.QUICK_STEER, e.startMs, e.speedAtStart)                 // 22
            lastLaneHalf = null
            return
        }
        if (e.speedAtStart < th.laneChangeMinKmh || e.peak >= th.turnStartDeg) return
        val last = lastLaneHalf
        if (last != null && last.sign != e.sign &&
            e.startMs - last.endMs <= (th.laneChangeWindowSec * 1000).toLong()
        ) {
            // S자 핸들 = 차선 변경으로 추정. 깜빡이가 한 번도 없었으면 묻는다.
            if (!last.signalSeen && !e.signalSeen) {
                ask(CoachItem.LANE_CHANGE_SIGNAL, last.startMs, e.speedAtStart)
            }
            lastLaneHalf = null
        } else {
            lastLaneHalf = LaneHalf(e.sign, e.startMs, s.timeMs, e.signalSeen)
        }
    }

    // ───────────────────── 가감속 (묻는 것) ─────────────────────

    /** 속도 기록을 갱신하고 [Thresholds.accelWindowMs] 구간의 평균 가속도(m/s²)를 돌려준다. */
    private fun pushSpeedAndGetAccel(s: VehicleSnapshot): Float {
        speedHistory.addLast(s.timeMs to s.speedKmh)
        val from = s.timeMs - th.accelWindowMs
        while (speedHistory.size >= 2 && speedHistory[1].first <= from) speedHistory.removeFirst()
        val (t0, v0) = speedHistory.first()
        val span = (s.timeMs - t0) / 1000f
        if (span < th.accelWindowMs / 1000f * 0.9f) return 0f
        return (s.speedKmh - v0) / 3.6f / span
    }

    private fun trackAccel(s: VehicleSnapshot, accel: Float) {
        if (accel <= -th.hardBrakeMps2) {
            if (hardBrakeArmed && s.gear != Gear.R) {
                ask(CoachItem.HARD_BRAKE, s.timeMs, speedHistory.first().second)  // 21
                hardBrakeArmed = false
            }
        } else if (accel > -th.hardBrakeMps2 / 2) {
            hardBrakeArmed = true
        }

        if (moveStartMs >= 0 && !hardStartAsked && accel >= th.hardStartMps2 &&
            s.timeMs - moveStartMs <= (th.hardStartWindowSec * 1000).toLong()
        ) {
            ask(CoachItem.HARD_START, moveStartMs, s.speedKmh)                     // 9
            hardStartAsked = true
        }
    }

    // ───────────────────────── 종료 ─────────────────────────

    private fun wrapUpDriving() {
        endStop()
        cancelWatch?.let { record(CoachItem.TURN_SIGNAL_CANCEL, miss = false) }
        cancelWatch = null
        turn = null
        excursion = null
    }

    private fun startClosing(s: VehicleSnapshot) {
        wrapUpDriving()
        closingMarker = missLog.size
        closingStartMs = s.timeMs
        closingParkingBrakeKnown = s.parkingBrakeOn != null
        closingParkingBrakeSeen = s.parkingBrakeOn == true
    }

    private fun handleClosing(p: VehicleSnapshot, s: VehicleSnapshot) {
        if (s.parkingBrakeOn != null) closingParkingBrakeKnown = true
        if (s.parkingBrakeOn == true) closingParkingBrakeSeen = true
        val doorOpened = s.doorOpen == true && p.doorOpen != true
        val timeUp = s.timeMs - closingStartMs >= (th.closingWindowSec * 1000).toLong()
        if (doorOpened || timeUp) finishInternal(s.timeMs)
    }

    private fun finishInternal(atMs: Long) {
        if (closingParkingBrakeKnown) {
            record(CoachItem.END_PARKING_BRAKE, miss = !closingParkingBrakeSeen)   // 44
        }
        if (doorKnown) {
            record(CoachItem.DOOR_OPEN, miss = doorOpenWhileMoving)                 // 1
            record(CoachItem.END_GEAR_P, miss = doorOpenNotInP)                     // 46
        }
        addEvent(if (closingStartMs >= 0) closingStartMs else atMs, DriveEvent.Kind.END, closingMarker)
        isFinished = true
        result = DriveResult(startMs, atMs, distance, tallies.toMap(), asks.toList(), events.sortedBy { it.atMs })
    }

    // ───────────────────────── 공통 ─────────────────────────

    private fun record(item: CoachItem, miss: Boolean) {
        val t = tallies[item] ?: ItemTally()
        tallies[item] = t + ItemTally(1, if (miss) 1 else 0)
        if (miss) missLog += item
    }

    private fun ask(item: CoachItem, atMs: Long, speedKmh: Float) {
        val moment = AskMoment(asks.size + 1, item, atMs, speedKmh)
        asks += moment
        events += DriveEvent(atMs, DriveEvent.Kind.ASK, ok = null, ask = moment)
    }
}

internal fun TurnSignal.isDirectional() = this == TurnSignal.LEFT || this == TurnSignal.RIGHT
