package com.bulletin.hyundai.core

/**
 * 기어 흐름에 맞춰 주행을 끊는다.
 * P 에서 벗어나면 주행 시작, 다시 P 로 와서 마무리 구간이 끝나면 결과를 넘긴다.
 */
class CoachSession(
    private val thresholds: Thresholds = Thresholds(),
    private val listener: Listener,
) {
    interface Listener {
        fun onDriveStarted()
        fun onDriveFinished(result: DriveResult)
    }

    private var evaluator: DriveEvaluator? = null
    var last: VehicleSnapshot? = null
        private set

    val isDriving: Boolean get() = evaluator != null

    fun onSnapshot(s: VehicleSnapshot) {
        last = s
        val ev = evaluator
        if (ev == null) {
            if (s.gear != Gear.P) {
                evaluator = DriveEvaluator(thresholds).also { it.onSnapshot(s) }
                listener.onDriveStarted()
            }
            return
        }
        ev.onSnapshot(s)
        if (ev.isFinished) deliver(ev)
    }

    /** 신호가 끊겼을 때 등, 지금까지 기록으로 주행을 마감한다. */
    fun abort(atMs: Long) {
        val ev = evaluator ?: return
        ev.forceFinish(atMs)
        deliver(ev)
    }

    private fun deliver(ev: DriveEvaluator) {
        evaluator = null
        ev.result?.let(listener::onDriveFinished)
    }
}
