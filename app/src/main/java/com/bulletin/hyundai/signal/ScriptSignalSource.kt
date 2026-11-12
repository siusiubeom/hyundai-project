package com.bulletin.hyundai.signal

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.bulletin.hyundai.core.DriveScript
import com.bulletin.hyundai.core.VehicleSignalSource
import com.bulletin.hyundai.core.VehicleSnapshot

/**
 * 시연 각본을 실제 시간에 맞춰 재생하는 가짜 신호원.
 * 플레오스 에뮬레이터가 붙으면 같은 인터페이스로 PleosVehicleSignalSource 를 만들어 바꿔 끼운다.
 *
 * @param speedFactor 2f 면 두 배 빠르게 재생 (판정에 쓰는 시각은 각본 시각 그대로)
 */
class ScriptSignalSource(
    private val script: DriveScript,
    private val speedFactor: Float = 1f,
    private val onEnd: () -> Unit,
) : VehicleSignalSource {

    private val handler = Handler(Looper.getMainLooper())
    private var frames: List<VehicleSnapshot> = emptyList()
    private var index = 0
    private var startWallMs = 0L
    private var callback: ((VehicleSnapshot) -> Unit)? = null

    private val tick = object : Runnable {
        override fun run() {
            val cb = callback ?: return
            val elapsed = ((SystemClock.uptimeMillis() - startWallMs) * speedFactor).toLong()
            val base = frames.first().timeMs
            while (index < frames.size && frames[index].timeMs - base <= elapsed) cb(frames[index++])
            if (index < frames.size) handler.postDelayed(this, FRAME_MS) else finish()
        }
    }

    override fun start(onSnapshot: (VehicleSnapshot) -> Unit) {
        frames = script.render(startMs = System.currentTimeMillis())
        index = 0
        callback = onSnapshot
        startWallMs = SystemClock.uptimeMillis()
        handler.post(tick)
    }

    override fun stop() {
        handler.removeCallbacks(tick)
        callback = null
    }

    private fun finish() {
        stop()
        onEnd()
    }

    private companion object {
        const val FRAME_MS = 50L
    }
}
