package com.bulletin.hyundai.core

/**
 * 앱이 다루는 항목. examNo = 도로교통법 시행규칙 별표 26 도로주행시험 채점 항목 번호.
 *
 * JUDGE = 상황과 무관하게 정답이 정해진 동작. 앱이 직접 판정한다.
 * ASK   = 상황에 따라 정답이 다름. 앱은 순간만 잡고 "무슨 일이었나요?"라고 묻는다.
 *
 * severity: 아쉬운 점과 "다음 운전 때 기억할 것"의 순서에만 쓴다(점수 아님).
 *   ※ 가안. 기획(별표 26 감점 크기)으로 확정해서 바꿀 것.
 */
enum class CoachItem(
    val examNo: String,
    val title: String,
    val kind: Kind,
    val severity: Int,
) {
    // ── ① 앱이 판단하는 것 ──
    SEATBELT("실격", "안전띠", Kind.JUDGE, 100),
    DOOR_OPEN("1", "차문 닫힘 확인", Kind.JUDGE, 80),
    PARKING_BRAKE_RELEASE("3", "주차 브레이크 풀고 출발", Kind.JUDGE, 70),
    TURN_SLOW("37", "회전할 때 서행", Kind.JUDGE, 60),
    TURN_SIGNAL_ON("30", "회전 전 깜빡이", Kind.JUDGE, 55),
    TURN_SIGNAL_EARLY("31", "깜빡이 30m 전에 켜기", Kind.JUDGE, 50),
    STOP_BRAKE("20", "정차 중 브레이크 유지", Kind.JUDGE, 45),
    END_GEAR_P("46", "내리기 전 기어 P", Kind.JUDGE, 40),
    END_PARKING_BRAKE("44", "주차 브레이크 걸고 마무리", Kind.JUDGE, 35),
    TURN_SIGNAL_HOLD("32", "회전 끝까지 깜빡이 유지", Kind.JUDGE, 30),
    TURN_SIGNAL_CANCEL("33", "회전 후 깜빡이 끄기", Kind.JUDGE, 25),
    COAST_NEUTRAL("18", "멈추기 전 기어 D 유지", Kind.JUDGE, 20),
    STOP_NEUTRAL("4", "오래 정차할 때 기어 N", Kind.JUDGE, 15),

    // ── ② 앱이 묻는 것 ── (답이 "내 실수"일 때만 아쉬운 점으로 센다)
    HARD_BRAKE("21", "급브레이크", Kind.ASK, 58),
    QUICK_STEER("22", "급한 핸들 조작", Kind.ASK, 52),
    HARD_START("9", "급출발", Kind.ASK, 42),
    LANE_CHANGE_SIGNAL("30", "차선 변경 깜빡이", Kind.ASK, 48),
    ;

    enum class Kind { JUDGE, ASK }
}
