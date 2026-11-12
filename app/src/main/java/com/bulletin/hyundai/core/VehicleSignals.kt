package com.bulletin.hyundai.core

/** 변속 레버 위치 */
enum class Gear { P, R, N, D }

/** 방향지시등 상태 */
enum class TurnSignal { NONE, LEFT, RIGHT, HAZARD }

/**
 * 한 시점의 차량 상태.
 *
 * 실제 차량(플레오스 Vehicle SDK)은 값마다 따로 콜백이 오므로, 어댑터가 최신 값을 모아
 * 이 스냅샷으로 만들어 넘긴다. 시간 기준 규칙(정차 10초 등)을 위해 값이 안 바뀌어도
 * 주기적으로(예: 100ms) 보내 주는 것을 전제로 한다.
 *
 * null = 이 차(또는 에뮬레이터)에서 읽을 수 없는 신호. 해당 항목은 판정하지 않는다.
 */
data class VehicleSnapshot(
    val timeMs: Long,
    val gear: Gear,
    val speedKmh: Float,
    /** 핸들 각도(도). 양수 = 왼쪽. 실제 부호는 에뮬레이터에서 확인 후 어댑터에서 맞출 것. */
    val steeringDeg: Float,
    val turnSignal: TurnSignal,
    val brakePressed: Boolean,
    val parkingBrakeOn: Boolean? = null,
    val doorOpen: Boolean? = null,
    val seatbeltOn: Boolean? = null,
)

/** 차량 신호 공급원. 시연 각본, 에뮬레이터, 실차 모두 이 뒤에 숨는다. */
interface VehicleSignalSource {
    fun start(onSnapshot: (VehicleSnapshot) -> Unit)
    fun stop()
}
