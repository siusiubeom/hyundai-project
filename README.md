# 옆자리

초보 운전자가 가족이나 친구와 연습할 때, 옆 사람이 하나하나 재기 어려운 기본 운전 동작을 차의 신호로 재고 정리해 다음 연습으로 이어주는 Pleos Connect 차량용 운전 연습 보조 앱입니다.

- 팀명: bulletin
- 앱 이름: 옆자리 (패키지 `com.bulletin.hyundai`)
- APK: [`apk/bulletin_옆자리_release.apk`](apk/)

## 주요 기능

| 시점 | 기어 | 앱이 하는 일 |
|---|---|---|
| 출발 전 | P | 지난 연습에서 정한 "다음 운전 때 기억할 것"을 최대 세 개 보여줍니다 |
| 주행 중 | D | 화면을 띄우지 않고 차량 신호를 기록만 합니다 (플레오스 주행 제한 규칙 준수) |
| 도착 후 | P | 점수 없이 "확인한 동작 N가지 중 M가지를 잘했어요", 잘한 점, 아쉬운 점, 같은 길 지난번과 비교, 확인 질문, 다음 운전 때 기억할 것을 보여줍니다 |

- **앱이 판정하는 항목(13개)**: 도로주행시험 채점기준(도로교통법 시행규칙 별표 26) 중 차량 신호로 잴 수 있고 상황과 무관하게 정답이 정해진 동작. 회전 전 깜빡이, 30m 전 깜빡이, 회전 끝까지 깜빡이 유지, 회전 후 깜빡이 끄기, 회전할 때 서행, 정차 중 브레이크 유지, 오래 정차할 때 기어 N, 멈추기 전 기어 D 유지, 차문 닫힘 확인, 주차 브레이크 풀고 출발, 주차 브레이크 걸고 마무리, 내리기 전 기어 P, 안전띠
- **앱이 묻는 항목(4개)**: 급브레이크, 급한 핸들 조작, 급출발, 깜빡이 없는 차선 변경. 상황에 따라 정답이 달라서 채점하지 않고 "무슨 일이었나요?"라고 묻습니다.

## 사용한 SDK / API

| 구분 | 사용 내용 | 권한 |
|---|---|---|
| Pleos Vehicle SDK 2.3.2 | 기어, 방향지시등, 조향각, 브레이크 페달, 도어, 안전띠 | `pleos.car.permission.CAR_DRIVE`, `CAR_LIGHTS`, `CAR_DOORS`, `CAR_SEATS`, `CAR_SAFETY` |
| Android 표준 차량 API (`CarPropertyManager`) | 속도(`PERF_VEHICLE_SPEED`), 주차 브레이크(`PARKING_BRAKE_ON`) | `android.car.permission.CAR_SPEED`, `CAR_POWERTRAIN` |

## 테스트 환경

- Pleos Connect 에뮬레이터 v2.1.0 (Android API 34, x86_64)
- Android Studio, Kotlin (Gradle 데몬 JDK 25)
- minSdk 28, targetSdk 37

## 실행 방법

### APK 설치

```bash
adb install -r -g "apk/bulletin_옆자리_release.apk"
```

`-g`는 차량 신호 권한을 설치 시 함께 허용합니다. 빼고 설치하면 첫 실행 때 권한을 요청합니다.

### 소스에서 빌드

```bash
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release.apk
```

플레오스 SDK는 `settings.gradle.kts`에 등록된 Maven 저장소(`https://nexus-playground.pleos.ai/repository/maven-releases/`)에서 받습니다.

### 시연 방법

1. **앱 안의 각본 재생**: 화면의 [각본 1회차], [각본 2회차] 버튼을 누르면 미리 짠 연습 주행이 재생되고, 끝나면 도착 후 리포트가 나옵니다. 2회차를 재생하면 같은 길 지난번과 비교가 함께 나옵니다. [기록 지우기]로 처음 상태로 돌아갑니다.
2. **에뮬레이터 가상 콘솔로 실제 신호 보내기**: 에뮬레이터를 켠 뒤 아래를 실행하면 출발 → 좌회전(깜빡이 O) → 우회전(깜빡이 X) → 정차 → P 흐름이 차량 신호로 들어갑니다.

   ```bash
   adb forward tcp:8088 tcp:8088
   bash tools/console_drive.sh
   ```

### 에뮬레이터 참고 사항

- 가상 콘솔에서 안전띠와 주차 브레이크 값은 바꿀 수 없어서, 에뮬레이터에서는 이 두 항목을 판정에서 뺍니다. 실차에서는 그대로 판정합니다.
- 기어 D에서는 플레오스 주행 제한으로 앱 화면이 가려지지만, 기록은 계속됩니다.

## 프로젝트 구조

```
app/src/main/java/com/bulletin/hyundai/
├── signal/   차량 신호 수신 (Vehicle SDK + CarPropertyManager, 시연 각본)
├── core/     주행 판정 엔진, 판정 기준값, 리포트 문장
├── data/     주행 기록·기억할 것 저장 (기기 안에만 저장)
└── ui/       출발 전 / 주행 중 / 도착 후 화면
tools/console_drive.sh   가상 콘솔로 연습 주행 재생
apk/                     심사용 release APK
```
