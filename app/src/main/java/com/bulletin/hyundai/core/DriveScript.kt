package com.bulletin.hyundai.core

/**
 * 시연용 연습 주행 각본. 속도·핸들은 키프레임 사이를 직선으로 잇고,
 * 기어·깜빡이 같은 단발 값은 [Event] 시점부터 유지한다.
 */
class DriveScript(
    val id: String,
    val title: String,
    val routeKey: String,
    val routeName: String,
    val durationSec: Float,
    /** (초, km/h) */
    private val speed: List<Pair<Float, Float>>,
    /** (초, 핸들 각도. 양수 = 왼쪽) */
    private val steer: List<Pair<Float, Float>>,
    private val events: List<Event>,
) {
    data class Event(
        val t: Float,
        val gear: Gear? = null,
        val signal: TurnSignal? = null,
        val brake: Boolean? = null,
        val parkingBrake: Boolean? = null,
        val door: Boolean? = null,
        val seatbelt: Boolean? = null,
    )

    fun render(stepMs: Long = 100, startMs: Long = 0): List<VehicleSnapshot> {
        val out = ArrayList<VehicleSnapshot>()
        var gear = Gear.P
        var signal = TurnSignal.NONE
        var brake = false
        var pb: Boolean? = null
        var door: Boolean? = null
        var belt: Boolean? = null
        val sorted = events.sortedBy { it.t }
        var ei = 0
        var ms = 0L
        val endMs = (durationSec * 1000).toLong()
        while (ms <= endMs) {
            val t = ms / 1000f
            while (ei < sorted.size && sorted[ei].t <= t + 1e-4f) {
                val e = sorted[ei++]
                e.gear?.let { gear = it }
                e.signal?.let { signal = it }
                e.brake?.let { brake = it }
                e.parkingBrake?.let { pb = it }
                e.door?.let { door = it }
                e.seatbelt?.let { belt = it }
            }
            out += VehicleSnapshot(
                timeMs = startMs + ms,
                gear = gear,
                speedKmh = lerp(speed, t),
                steeringDeg = lerp(steer, t),
                turnSignal = signal,
                brakePressed = brake,
                parkingBrakeOn = pb,
                doorOpen = door,
                seatbeltOn = belt,
            )
            ms += stepMs
        }
        return out
    }

    private fun lerp(points: List<Pair<Float, Float>>, t: Float): Float {
        if (points.isEmpty()) return 0f
        if (t <= points.first().first) return points.first().second
        for (i in 1 until points.size) {
            val (t1, v1) = points[i]
            if (t <= t1) {
                val (t0, v0) = points[i - 1]
                if (t1 == t0) return v1
                return v0 + (v1 - v0) * (t - t0) / (t1 - t0)
            }
        }
        return points.last().second
    }
}

/**
 * 공모전 시연 각본. 같은 길(집 → 마트)을 두 번 달려 "지난번과 비교"까지 보여준다.
 *
 * 1회차: 정상 좌회전 → 깜빡이 없이 빠른 우회전 → 돌발 회피(급제동 + 급핸들) → 정차 → P
 * 2회차: 두 회전 모두 깜빡이 OK, 서행 OK. 대신 우회전 후 깜빡이를 안 끔.
 */
object DemoScripts {

    private val home = Pair("home-mart", "집 → 마트")

    val RUN_1 = DriveScript(
        id = "run1",
        title = "1회차",
        routeKey = home.first,
        routeName = home.second,
        durationSec = 48f,
        speed = listOf(
            0f to 0f, 2.5f to 0f, 7f to 30f, 10f to 30f, 12f to 15f, 16f to 15f,
            19f to 35f, 22f to 35f, 23.5f to 25f, 27f to 25f, 29f to 40f, 30f to 40f,
            31f to 5f, 32f to 0f,
        ),
        steer = listOf(
            0f to 0f,
            12f to 0f, 13f to 360f, 15f to 360f, 16f to 0f,          // 좌회전
            23.5f to 0f, 24.5f to -360f, 26f to -360f, 27f to 0f,   // 우회전 (깜빡이 없음)
            30.2f to 0f, 30.5f to 70f, 30.9f to 0f,                 // 돌발 회피
        ),
        events = listOf(
            DriveScript.Event(0f, gear = Gear.P, brake = true, parkingBrake = true, door = false, seatbelt = true),
            DriveScript.Event(1f, parkingBrake = false),
            DriveScript.Event(1.5f, gear = Gear.D),
            DriveScript.Event(2.5f, brake = false),
            DriveScript.Event(7f, signal = TurnSignal.LEFT),
            DriveScript.Event(10f, brake = true),
            DriveScript.Event(12f, brake = false),
            DriveScript.Event(17f, signal = TurnSignal.NONE),
            DriveScript.Event(22.5f, brake = true),
            DriveScript.Event(23.5f, brake = false),
            DriveScript.Event(30.9f, brake = true),
            DriveScript.Event(33f, gear = Gear.N),
            DriveScript.Event(44f, gear = Gear.P),
            DriveScript.Event(45f, parkingBrake = true),
            DriveScript.Event(47f, door = true),
        ),
    )

    val RUN_2 = DriveScript(
        id = "run2",
        title = "2회차",
        routeKey = home.first,
        routeName = home.second,
        durationSec = 50f,
        speed = listOf(
            0f to 0f, 2.5f to 0f, 7f to 30f, 10f to 30f, 12f to 15f, 16f to 15f,
            19f to 35f, 22f to 35f, 23.5f to 15f, 27f to 15f, 29f to 35f, 34f to 0f,
        ),
        steer = listOf(
            0f to 0f,
            12f to 0f, 13f to 360f, 15f to 360f, 16f to 0f,          // 좌회전
            23.5f to 0f, 24.5f to -360f, 26f to -360f, 27f to 0f,   // 우회전
        ),
        events = listOf(
            DriveScript.Event(0f, gear = Gear.P, brake = true, parkingBrake = true, door = false, seatbelt = true),
            DriveScript.Event(1f, parkingBrake = false),
            DriveScript.Event(1.5f, gear = Gear.D),
            DriveScript.Event(2.5f, brake = false),
            DriveScript.Event(7f, signal = TurnSignal.LEFT),
            DriveScript.Event(10f, brake = true),
            DriveScript.Event(12f, brake = false),
            DriveScript.Event(17f, signal = TurnSignal.NONE),
            DriveScript.Event(19f, signal = TurnSignal.RIGHT),
            DriveScript.Event(22.5f, brake = true),
            DriveScript.Event(23.5f, brake = false),
            DriveScript.Event(30f, brake = true),
            DriveScript.Event(31f, signal = TurnSignal.NONE),       // 너무 늦게 끔
            DriveScript.Event(35f, gear = Gear.N),
            DriveScript.Event(46f, gear = Gear.P),
            DriveScript.Event(47f, parkingBrake = true),
            DriveScript.Event(49f, door = true),
        ),
    )

    val ALL = listOf(RUN_1, RUN_2)
}
