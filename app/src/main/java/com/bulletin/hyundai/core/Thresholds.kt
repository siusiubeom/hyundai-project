package com.bulletin.hyundai.core

/**
 * 판정 기준 숫자. 전부 가안이다.
 * 에뮬레이터에서 값의 단위·범위·주기를 확인한 뒤 기획에서 숫자로 확정한다.
 */
data class Thresholds(
    // 회전 (교차로 좌·우회전). 핸들(스티어링 휠) 각도 기준.
    /** 이 각도 이상 돌리면 회전 시작으로 본다 */
    val turnStartDeg: Float = 90f,
    /** 이 각도 미만으로 돌아오면 회전 끝으로 본다 */
    val turnEndDeg: Float = 20f,
    /** 회전으로 보려면 이 속도 이상으로 움직이고 있어야 한다 (제자리 핸들 제외) */
    val turnMinSpeedKmh: Float = 3f,
    /** 회전 중 이 속도를 넘으면 서행 위반 (37) */
    val turnSlowMaxKmh: Float = 20f,
    /** 깜빡이를 켠 뒤 회전까지 최소 거리 (31) */
    val signalLeadMeters: Float = 30f,
    /** 회전 후 깜빡이를 켠 채 이 거리 이상 달리면 미중지 (33) */
    val signalCancelMeters: Float = 10f,

    // 정차
    /** 이 속도 미만이면 정차 */
    val stoppedKmh: Float = 0.5f,
    /** 정차 후 이 속도 이상이면 다시 출발한 것으로 본다 */
    val movingKmh: Float = 2f,
    /** 이 시간 이상 정차하면 기어 N 이어야 한다 (4) */
    val stopNeutralSec: Float = 10f,
    /** 이 속도 이상으로 움직이는 중 기어 N 이면 엔진브레이크 미숙 (18) */
    val coastNeutralMinKmh: Float = 5f,
    /** 정차 중 D 에서 브레이크를 이 시간 넘게 떼면 미제동 (20) */
    val stopNoBrakeSec: Float = 1f,

    // 애매한 순간 (묻는 것)
    /** 이 감속도(m/s²) 이상이면 급제동으로 표시. 0.4g 근처. */
    val hardBrakeMps2: Float = 4.0f,
    /** 정지 상태에서 출발 직후 이 가속도(m/s²) 이상이면 급출발로 표시 */
    val hardStartMps2: Float = 3.0f,
    /** 출발 뒤 이 시간 안의 가속만 급출발로 본다 */
    val hardStartWindowSec: Float = 3f,
    /** 가속도 계산에 쓰는 구간 */
    val accelWindowMs: Long = 1000,
    /** 핸들 움직임 시작으로 보는 각도 */
    val steerExcursionDeg: Float = 8f,
    /** 급핸들: 최소 꺾은 각도, 최소 각속도, 최대 지속시간, 최소 속도 */
    val quickSteerMinDeg: Float = 45f,
    val quickSteerDegPerSec: Float = 200f,
    val quickSteerMaxSec: Float = 1.5f,
    val quickSteerMinKmh: Float = 20f,
    /** 차선 변경(S자): 이 속도 이상에서, 반대 방향 핸들이 이 시간 안에 이어지면 */
    val laneChangeMinKmh: Float = 30f,
    val laneChangeWindowSec: Float = 4f,

    // 종료
    /** P 로 바꾼 뒤 주차 브레이크를 기다리는 시간. 문이 열리면 바로 끝낸다. */
    val closingWindowSec: Float = 5f,
)
