#!/usr/bin/env bash
# 플레오스 에뮬레이터 가상 콘솔로 연습 주행 한 번을 재생한다.
# 사전 준비: 에뮬레이터 실행 + `adb forward tcp:8088 tcp:8088`
# 흐름: 출발 → 좌회전(깜빡이 O, 시속 15) → 우회전(깜빡이 X, 시속 25) → 정차 → P
# 콘솔 핸들 각도: + = 왼쪽 (차량 PERF_STEERING_ANGLE 에서는 부호가 반대로 들어온다)
C=${CONSOLE:-http://127.0.0.1:8088}
put(){ curl -s -o /dev/null -X PUT "$C/vehicle/$1"; }
ctl(){ put "controls?speed=$1&accelerator=0&brake=$2&steering=$3"; }

# 콘솔은 브라우저 세션 하트비트(3초)가 없으면 입력을 받지 않는다
( while true; do curl -s -o /dev/null -X POST "$C/browser/session?client=coach-cli"; sleep 1; done ) &
HB=$!
trap 'kill $HB 2>/dev/null' EXIT
sleep 1

put "turn-signal?mode=Off"; ctl 0 60 0; put "gear?gear=Park"; sleep 7   # 이전 주행 마감
put "gear?gear=Drive"; sleep 1
for v in 4 8 12 16 20 24 28 30; do ctl $v 0 0; sleep 0.5; done
put "turn-signal?mode=BlinkLeft"; sleep 4                                 # 약 33m 전 깜빡이
for v in 26 22 18 15; do ctl $v 30 0; sleep 0.5; done
for a in 90 180 270 360; do ctl 15 0 $a; sleep 0.3; done; sleep 1.5      # 좌회전
for a in 270 180 90 0; do ctl 15 0 $a; sleep 0.3; done; sleep 0.8
put "turn-signal?mode=Off"
for v in 18 21 24 27 30; do ctl $v 0 0; sleep 0.5; done; sleep 2
for v in 28 25; do ctl $v 20 0; sleep 0.5; done
for a in -90 -180 -270 -360; do ctl 25 0 $a; sleep 0.3; done; sleep 1.5  # 우회전: 깜빡이 X, 시속 25
for a in -270 -180 -90 0; do ctl 25 0 $a; sleep 0.3; done
for v in 22 19 16 13 10 7 4 1 0; do ctl $v 50 0; sleep 0.5; done; sleep 2
put "gear?gear=Park"; sleep 7                                              # 마무리 구간(5초) 후 리포트
