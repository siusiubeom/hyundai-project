package com.bulletin.hyundai.core

import com.bulletin.hyundai.core.CoachItem.*

/**
 * 리포트에 나가는 모든 문장. 기획에서 이 파일만 고치면 된다.
 * 말투 원칙: 지적이 아니라 다음 행동을 알려준다.
 */
object Phrases {

    data class AskOption(val text: String, val myMistake: Boolean)

    fun good(item: CoachItem, t: ItemTally): String = when (item) {
        SEATBELT -> "출발 전에 안전띠를 맸어요."
        DOOR_OPEN -> "문이 모두 닫힌 상태로 달렸어요."
        PARKING_BRAKE_RELEASE -> "주차 브레이크를 풀고 출발했어요."
        TURN_SLOW -> "회전 ${t.opportunities}번 모두 천천히 돌았어요."
        TURN_SIGNAL_ON -> "회전 ${t.opportunities}번 모두 깜빡이를 켜고 돌았어요."
        TURN_SIGNAL_EARLY -> "깜빡이를 회전하기 충분히 전에 켰어요."
        STOP_BRAKE -> "멈춰 있는 동안 브레이크를 꾹 밟고 있었어요."
        END_GEAR_P -> "기어를 P에 두고 문을 열었어요."
        END_PARKING_BRAKE -> "주차 브레이크를 걸고 마무리했어요."
        TURN_SIGNAL_HOLD -> "회전이 끝날 때까지 깜빡이를 유지했어요."
        TURN_SIGNAL_CANCEL -> "회전 후 깜빡이를 잘 껐어요."
        COAST_NEUTRAL -> "멈출 때까지 기어를 D에 두었어요."
        STOP_NEUTRAL -> "오래 서 있을 때 기어를 N으로 바꿨어요."
        HARD_BRAKE -> "급하게 멈춘 순간은 상황에 맞는 대응이었어요."
        QUICK_STEER -> "급한 핸들은 피해야 할 상황에 맞춘 조작이었어요."
        HARD_START -> "빠른 출발은 상황에 필요한 가속이었어요."
        LANE_CHANGE_SIGNAL -> "깜빡이 없이 차선을 바꾼 적은 없었어요."
    }

    fun miss(item: CoachItem, t: ItemTally, th: Thresholds): String {
        val n = t.opportunities
        val m = t.misses
        return when (item) {
            SEATBELT -> "안전띠를 매기 전에 기어를 넣었어요."
            DOOR_OPEN -> "문이 열린 채로 차가 움직였어요."
            PARKING_BRAKE_RELEASE -> "주차 브레이크가 걸린 채 출발했어요."
            TURN_SLOW -> "회전 ${n}번 중 ${m}번은 시속 ${th.turnSlowMaxKmh.toInt()}km보다 빠르게 돌았어요."
            TURN_SIGNAL_ON -> "회전 ${n}번 중 ${m}번은 깜빡이 없이 돌았어요."
            TURN_SIGNAL_EARLY -> "깜빡이를 켠 회전 ${n}번 중 ${m}번은 회전 직전에 켰어요."
            STOP_BRAKE -> "정차 ${n}번 중 ${m}번은 D에 둔 채 브레이크에서 발을 뗐어요."
            END_GEAR_P -> "기어를 P에 두기 전에 문을 열었어요."
            END_PARKING_BRAKE -> "도착 후 주차 브레이크를 걸지 않았어요."
            TURN_SIGNAL_HOLD -> "회전 ${n}번 중 ${m}번은 다 돌기 전에 깜빡이가 꺼졌어요."
            TURN_SIGNAL_CANCEL -> "회전 ${n}번 중 ${m}번은 다 돈 뒤에도 깜빡이가 켜져 있었어요."
            COAST_NEUTRAL -> "정차 ${n}번 중 ${m}번은 멈추기 전에 미리 N으로 바꿨어요."
            STOP_NEUTRAL -> "${th.stopNeutralSec.toInt()}초 넘게 선 ${n}번 중 ${m}번은 기어를 N으로 바꾸지 않았어요."
            HARD_BRAKE -> "급하게 멈춘 ${n}번 중 ${m}번은 멈출 곳을 늦게 봤어요."
            QUICK_STEER -> "핸들을 급하게 움직인 ${n}번 중 ${m}번은 생각보다 많이 돌렸어요."
            HARD_START -> "빠르게 출발한 ${n}번 중 ${m}번은 페달을 세게 밟았어요."
            LANE_CHANGE_SIGNAL -> "차선 변경 ${n}번 중 ${m}번은 깜빡이 없이 바꿨어요."
        }
    }

    /** 다음 운전 때 기억할 것 (다음 출발 전에 그대로 다시 보여준다) */
    fun tip(item: CoachItem): String = when (item) {
        SEATBELT -> "기어를 D에 넣기 전에 안전띠부터 매요."
        DOOR_OPEN -> "출발 전에 계기판의 문 열림 표시가 꺼졌는지 봐요."
        PARKING_BRAKE_RELEASE -> "출발 전에 주차 브레이크 표시등이 꺼졌는지 확인해요."
        TURN_SLOW -> "회전하기 전에 미리 속도를 줄이고, 도는 동안은 가속 페달을 살짝만 밟아요."
        TURN_SIGNAL_ON -> "핸들을 돌리기 전에 깜빡이부터 켜요."
        TURN_SIGNAL_EARLY -> "회전할 곳이 보이면 30m(차 6~7대 길이) 전에 깜빡이를 켜요."
        STOP_BRAKE -> "D에 두고 서 있을 땐 출발할 때까지 브레이크를 계속 밟아요."
        END_GEAR_P -> "문을 열기 전에 기어가 P인지 먼저 봐요."
        END_PARKING_BRAKE -> "P에 넣은 다음 주차 브레이크까지 걸고 내려요."
        TURN_SIGNAL_HOLD -> "깜빡이는 회전이 다 끝날 때까지 그대로 둬요."
        TURN_SIGNAL_CANCEL -> "회전을 마치고 핸들을 풀면 깜빡이가 꺼졌는지 확인해요."
        COAST_NEUTRAL -> "멈출 때는 D에 둔 채 브레이크로 속도를 줄여요."
        STOP_NEUTRAL -> "신호 대기처럼 오래 설 때는 기어를 N으로 바꿔요."
        HARD_BRAKE -> "멈출 곳이 보이면 일찍부터 브레이크를 살짝 밟기 시작해요."
        QUICK_STEER -> "핸들은 두 손으로 잡고, 조금씩 나눠서 돌려요."
        HARD_START -> "출발할 땐 가속 페달을 천천히, 조금씩 밟아요."
        LANE_CHANGE_SIGNAL -> "차선을 바꾸기 전에 깜빡이를 켜고, 거울로 확인한 뒤 옮겨요."
    }

    fun question(item: CoachItem, whenLabel: String): String = when (item) {
        HARD_BRAKE -> "${whenLabel}에 속도가 갑자기 줄었어요. 무슨 일이었나요?"
        QUICK_STEER -> "${whenLabel}에 핸들을 빠르게 꺾었다 되돌렸어요. 무슨 일이었나요?"
        HARD_START -> "${whenLabel}에 출발이 꽤 빨랐어요. 무슨 일이었나요?"
        LANE_CHANGE_SIGNAL -> "${whenLabel}에 깜빡이 없이 차선을 바꾼 것 같아요. 맞나요?"
        else -> "${whenLabel}에 무슨 일이 있었나요?"
    }

    /** 이지선다. 두 번째가 "내 실수" 쪽. */
    fun options(item: CoachItem): List<AskOption> = when (item) {
        HARD_BRAKE -> listOf(
            AskOption("앞에 갑자기 무언가 나타났어요", false),
            AskOption("멈출 곳을 늦게 봤어요", true),
        )
        QUICK_STEER -> listOf(
            AskOption("무언가를 피하려고 했어요", false),
            AskOption("핸들이 생각보다 많이 돌아갔어요", true),
        )
        HARD_START -> listOf(
            AskOption("빨리 출발해야 하는 상황이었어요", false),
            AskOption("페달을 생각보다 세게 밟았어요", true),
        )
        LANE_CHANGE_SIGNAL -> listOf(
            AskOption("차선을 바꾸지 않았어요", false),
            AskOption("깜빡이 없이 바꿨어요", true),
        )
        else -> listOf(AskOption("상황 때문이었어요", false), AskOption("제 실수였어요", true))
    }

    fun headline(checked: Int, good: Int): String =
        if (checked == 0) "오늘은 확인할 상황이 없었어요"
        else "오늘 확인한 동작 ${checked}가지 중 ${good}가지를 잘했어요"

    fun elapsedLabel(ms: Long): String {
        val sec = (ms / 1000).coerceAtLeast(0)
        return if (sec < 60) "출발 ${sec}초쯤" else "출발 ${sec / 60}분 ${sec % 60}초쯤"
    }
}
