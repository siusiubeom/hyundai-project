package com.bulletin.hyundai.core

import com.bulletin.hyundai.core.CoachItem.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveEvaluatorTest {

    private fun run(script: DriveScript): DriveResult {
        val results = mutableListOf<DriveResult>()
        val session = CoachSession(listener = object : CoachSession.Listener {
            override fun onDriveStarted() {}
            override fun onDriveFinished(result: DriveResult) { results += result }
        })
        script.render().forEach(session::onSnapshot)
        assertNotNull("주행이 마감되지 않음", results.singleOrNull())
        return results.single()
    }

    private fun DriveResult.t(item: CoachItem) = tallies[item] ?: ItemTally()

    @Test
    fun run1_judgedItems() {
        val r = run(DemoScripts.RUN_1)
        assertEquals(ItemTally(2, 1), r.t(TURN_SIGNAL_ON))     // 우회전 깜빡이 없음
        assertEquals(ItemTally(2, 1), r.t(TURN_SLOW))          // 우회전 시속 25
        assertEquals(ItemTally(1, 0), r.t(TURN_SIGNAL_EARLY))  // 좌회전 약 38m 전
        assertEquals(ItemTally(1, 0), r.t(TURN_SIGNAL_HOLD))
        assertEquals(ItemTally(1, 0), r.t(TURN_SIGNAL_CANCEL))
        assertEquals(ItemTally(1, 0), r.t(SEATBELT))
        assertEquals(ItemTally(1, 0), r.t(PARKING_BRAKE_RELEASE))
        assertEquals(ItemTally(1, 0), r.t(COAST_NEUTRAL))
        assertEquals(ItemTally(1, 0), r.t(STOP_BRAKE))
        assertEquals(ItemTally(1, 0), r.t(STOP_NEUTRAL))
        assertEquals(ItemTally(1, 0), r.t(END_PARKING_BRAKE))
        assertEquals(ItemTally(1, 0), r.t(DOOR_OPEN))
        assertEquals(ItemTally(1, 0), r.t(END_GEAR_P))
    }

    @Test
    fun run1_asksHardBrakeAndQuickSteer() {
        val r = run(DemoScripts.RUN_1)
        val kinds = r.asks.map { it.item }
        assertEquals(listOf(QUICK_STEER, HARD_BRAKE).sorted(), kinds.sorted())
    }

    @Test
    fun run2_onlyCancelMissed() {
        val r = run(DemoScripts.RUN_2)
        assertEquals(ItemTally(2, 0), r.t(TURN_SIGNAL_ON))
        assertEquals(ItemTally(2, 0), r.t(TURN_SLOW))
        assertEquals(ItemTally(2, 0), r.t(TURN_SIGNAL_EARLY))
        assertEquals(ItemTally(2, 1), r.t(TURN_SIGNAL_CANCEL))
        assertTrue("급제동 없어야 함: ${r.asks}", r.asks.isEmpty())
        val missed = r.tallies.filterValues { it.misses > 0 }.keys
        assertEquals(setOf(TURN_SIGNAL_CANCEL), missed)
    }

    @Test
    fun lateSignal_isMissed() {
        // 깜빡이를 회전 1초 전(시속 15 → 약 4m)에 켬
        val s = DriveScript(
            "t", "t", "k", "k", 22f,
            speed = listOf(0f to 0f, 1f to 0f, 4f to 15f, 12f to 15f, 14f to 0f),
            steer = listOf(0f to 0f, 6f to 0f, 7f to 200f, 8f to 200f, 9f to 0f),
            events = listOf(
                DriveScript.Event(0f, gear = Gear.D),
                DriveScript.Event(5.6f, signal = TurnSignal.LEFT),
                DriveScript.Event(9.5f, signal = TurnSignal.NONE),
                DriveScript.Event(16f, gear = Gear.P),
            ),
        )
        val r = run(s)
        assertEquals(ItemTally(1, 0), r.t(TURN_SIGNAL_ON))
        assertEquals(ItemTally(1, 1), r.t(TURN_SIGNAL_EARLY))
    }

    @Test
    fun parkingBrakeOnAtStart_andNoBrakeAtStop() {
        val s = DriveScript(
            "t", "t", "k", "k", 20f,
            speed = listOf(0f to 0f, 1f to 0f, 4f to 20f, 6f to 20f, 8f to 0f),
            steer = listOf(0f to 0f),
            events = listOf(
                DriveScript.Event(0f, gear = Gear.D, parkingBrake = true, brake = false),
                DriveScript.Event(10f, gear = Gear.P),
            ),
        )
        val r = run(s)
        assertEquals(ItemTally(1, 1), r.t(PARKING_BRAKE_RELEASE))
        assertEquals(ItemTally(1, 1), r.t(STOP_BRAKE))
    }

    @Test
    fun laneChangeWithoutSignal_isAsked() {
        val s = DriveScript(
            "t", "t", "k", "k", 36f,
            speed = listOf(0f to 0f, 1f to 0f, 11f to 50f, 18f to 50f, 28f to 0f),
            steer = listOf(0f to 0f, 12f to 0f, 13f to 15f, 14f to 0f, 14.5f to -15f, 15.5f to 0f),
            events = listOf(
                DriveScript.Event(0f, gear = Gear.D, brake = true),
                DriveScript.Event(29.5f, gear = Gear.P),
            ),
        )
        val r = run(s)
        assertEquals(listOf(LANE_CHANGE_SIGNAL), r.asks.map { it.item })
    }

    @Test
    fun report_countsAnswersAndBuildsMemory() {
        val r = run(DemoScripts.RUN_1)
        val writer = ReportWriter()
        val brakeAsk = r.asks.first { it.item == HARD_BRAKE }
        val report = writer.build(r, "집 → 마트", answers = mapOf(brakeAsk.id to true))
        // 판정 13개 + 답한 질문 1개 = 14개, 놓친 것 = 깜빡이·서행·급브레이크 3개
        assertEquals("오늘 확인한 동작 14가지 중 11가지를 잘했어요", report.headline)
        assertEquals(listOf(TURN_SLOW, HARD_BRAKE, TURN_SIGNAL_ON), report.memory.map { it.item })
    }

    @Test
    fun report_comparesSameRouteByRatio() {
        val writer = ReportWriter()
        val r1 = run(DemoScripts.RUN_1)
        val rec1 = DriveRecord(r1.startMs, "home-mart", "집 → 마트", writer.effectiveTallies(r1, emptyMap()))
        val r2 = run(DemoScripts.RUN_2)
        val report = writer.build(r2, "집 → 마트", history = listOf(rec1))
        assertEquals(2, report.runNumber)
        assertTrue(report.comparisons.any { it.text.startsWith("회전 전 깜빡이: 놓친 비율 50% → 0%") })
        assertTrue(report.comparisons.any { it.text.startsWith("회전 후 깜빡이 끄기: 놓친 비율 0% → 50%") })
    }

    @Test
    fun run1_timeline() {
        val r = run(DemoScripts.RUN_1)
        assertEquals(
            listOf("START:true", "TURN_LEFT:true", "TURN_RIGHT:false", "ASK:null", "ASK:null", "STOP:true", "END:true"),
            r.events.map { "${it.kind}:${it.ok}" },
        )
        assertEquals(setOf(TURN_SIGNAL_ON, TURN_SLOW), r.events[2].missed.toSet())
    }

    @Test
    fun run2_cancelMissAttachedToRightTurn() {
        val r = run(DemoScripts.RUN_2)
        val right = r.events.first { it.kind == DriveEvent.Kind.TURN_RIGHT }
        assertEquals(false, right.ok)
        assertEquals(listOf(TURN_SIGNAL_CANCEL), right.missed)
    }
}
