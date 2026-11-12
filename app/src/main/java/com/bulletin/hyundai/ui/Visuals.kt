package com.bulletin.hyundai.ui

import com.bulletin.hyundai.R
import com.bulletin.hyundai.core.CoachItem
import com.bulletin.hyundai.core.CoachItem.*
import com.bulletin.hyundai.core.DriveEvent

/** 항목·장면별 그림과 짧은 이름 */
object Visuals {

    fun icon(item: CoachItem): Int = when (item) {
        SEATBELT -> R.drawable.ic_seatbelt
        DOOR_OPEN -> R.drawable.ic_door
        PARKING_BRAKE_RELEASE, END_PARKING_BRAKE -> R.drawable.ic_parking_brake
        TURN_SLOW -> R.drawable.ic_speed
        TURN_SIGNAL_ON, TURN_SIGNAL_EARLY, TURN_SIGNAL_HOLD, TURN_SIGNAL_CANCEL -> R.drawable.ic_signal
        STOP_BRAKE -> R.drawable.ic_brake
        END_GEAR_P -> R.drawable.ic_gear_p
        COAST_NEUTRAL, STOP_NEUTRAL -> R.drawable.ic_gear_n
        HARD_BRAKE -> R.drawable.ic_hard_brake
        QUICK_STEER -> R.drawable.ic_quick_steer
        HARD_START -> R.drawable.ic_hard_start
        LANE_CHANGE_SIGNAL -> R.drawable.ic_lane_change
    }

    fun icon(e: DriveEvent): Int = when (e.kind) {
        DriveEvent.Kind.START -> R.drawable.ic_start
        DriveEvent.Kind.TURN_LEFT -> R.drawable.ic_turn_left
        DriveEvent.Kind.TURN_RIGHT -> R.drawable.ic_turn_right
        DriveEvent.Kind.STOP -> R.drawable.ic_stop
        DriveEvent.Kind.ASK -> e.ask?.let { icon(it.item) } ?: R.drawable.ic_question
        DriveEvent.Kind.END -> R.drawable.ic_flag
    }

    fun label(e: DriveEvent): String = when (e.kind) {
        DriveEvent.Kind.START -> "출발"
        DriveEvent.Kind.TURN_LEFT -> "좌회전"
        DriveEvent.Kind.TURN_RIGHT -> "우회전"
        DriveEvent.Kind.STOP -> "정차"
        DriveEvent.Kind.ASK -> e.ask?.item?.title ?: "확인"
        DriveEvent.Kind.END -> "도착"
    }
}
