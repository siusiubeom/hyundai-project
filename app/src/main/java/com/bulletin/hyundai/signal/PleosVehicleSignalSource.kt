package com.bulletin.hyundai.signal

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.constant.area.DoorValue
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.BrakePedalStatusListener
import ai.pleos.playground.vehicle.listener.DoorPositionListener
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import ai.pleos.playground.vehicle.listener.SeatBeltBuckledStateListener
import ai.pleos.playground.vehicle.listener.SteeringWheelAngleListener
import ai.pleos.playground.vehicle.listener.TurnSignalStateListener
import com.bulletin.hyundai.core.Gear
import com.bulletin.hyundai.core.TurnSignal
import com.bulletin.hyundai.core.VehicleSignalSource
import com.bulletin.hyundai.core.VehicleSnapshot
import kotlin.math.abs

/**
 * 플레오스 Connect 차량 신호 → [VehicleSnapshot].
 *
 * - 기어·깜빡이·핸들·브레이크 페달·문·안전띠: 플레오스 Vehicle SDK
 * - 속도·주차 브레이크: Vehicle SDK 에 없어 안드로이드 표준 CarPropertyManager 사용
 *
 * 값마다 콜백이 따로 오므로 최신 값을 모아 두고, [TICK_MS] 마다 스냅샷을 보낸다.
 * 읽지 못한 신호는 null 로 두어 해당 항목을 판정하지 않게 한다.
 *
 * @param steeringSign 핸들 각도 부호 보정. 코어는 "양수 = 왼쪽"을 쓰고, 안드로이드 차량 표준
 *   PERF_STEERING_ANGLE 은 "왼쪽 = 음수"라 기본값 -1. (플레오스 에뮬레이터에서 확인: 콘솔 왼쪽 +360 → -360)
 */
class PleosVehicleSignalSource(
    private val context: Context,
    private val steeringSign: Float = -1f,
    /** 읽히지만 바꿀 수 없는 신호(에뮬레이터의 안전띠·주차 브레이크)는 판정에서 뺀다 */
    private val ignored: Set<String> = emptySet(),
) : VehicleSignalSource {

    private val handler = Handler(Looper.getMainLooper())
    private var callback: ((VehicleSnapshot) -> Unit)? = null

    private var vehicle: Vehicle? = null
    private var car: Car? = null
    private var props: CarPropertyManager? = null

    // 최신 값 (메인 스레드에서만 읽고 쓴다)
    private var gear = Gear.P
    private var speedKmh = 0f
    private var steeringDeg = 0f
    private var turnSignal = TurnSignal.NONE
    private var brake = false
    private var parkingBrake: Boolean? = null
    private var doorOpen: Boolean? = null
    private var seatbelt: Boolean? = null

    /** 신호별 마지막 수신 시각. 시연용 신호 패널에서 "들어오는지" 확인할 때 쓴다. */
    val lastReceived = mutableMapOf<String, Long>()

    private val driverDoor = DoorArea(DoorValue.ROW_1_LEFT)
    private val driverSeat = SeatArea(SeatValue.ROW_1_LEFT)

    private fun mark(name: String) {
        lastReceived[name] = System.currentTimeMillis()
    }

    private fun onMain(block: () -> Unit) {
        handler.post(block)
    }

    private fun fail(name: String): (Exception) -> Unit = { e -> Log.w(TAG, "$name 수신 실패", e) }

    // ── Vehicle SDK 리스너 ──

    private val gearListener = object : GearSelectionListener {
        override fun onSelected(value: VehicleGear?) = onMain {
            mark("gear")
            gear = when (value) {
                VehicleGear.GEAR_PARK -> Gear.P
                VehicleGear.GEAR_REVERSE -> Gear.R
                VehicleGear.GEAR_NEUTRAL -> Gear.N
                VehicleGear.GEAR_UNKNOWN, null -> gear
                else -> Gear.D // GEAR_DRIVE, GEAR_1..9
            }
        }
        override fun onFailed(e: Exception) = fail("gear")(e)
    }

    private val turnListener = object : TurnSignalStateListener {
        override fun onTurnSignalStateUpdated(value: TurnSignalValue?) = onMain {
            mark("turnSignal")
            turnSignal = when (value) {
                TurnSignalValue.LEFT -> TurnSignal.LEFT
                TurnSignalValue.RIGHT -> TurnSignal.RIGHT
                else -> TurnSignal.NONE
            }
        }
        override fun onFailed(e: Exception) = fail("turnSignal")(e)
    }

    private val steeringListener = object : SteeringWheelAngleListener {
        override fun onSteeringWheelAngleUpdated(value: Float?) = onMain {
            mark("steering")
            if (value != null) steeringDeg = value * steeringSign
        }
        override fun onFailed(e: Exception) = fail("steering")(e)
    }

    private val brakeListener = object : BrakePedalStatusListener {
        override fun onBrakePedalStatusUpdated(value: Boolean?) = onMain {
            mark("brake")
            if (value != null) brake = value
        }
        override fun onFailed(e: Exception) = fail("brake")(e)
    }

    private val doorListener = object : DoorPositionListener {
        override fun onPositionUpdated(value: Int?) = onMain {
            mark("door")
            if (value != null) doorOpen = value > 0
        }
        override fun onFailed(e: Exception) = fail("door")(e)
    }

    private val beltListener = object : SeatBeltBuckledStateListener {
        override fun onSeatBeltBuckledStateUpdated(value: Boolean?) = onMain {
            mark("seatbelt")
            if (value != null) seatbelt = value
        }
        override fun onFailed(e: Exception) = fail("seatbelt")(e)
    }

    // ── 안드로이드 표준 차량 속성 (속도, 주차 브레이크) ──

    private val propCallback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(v: CarPropertyValue<*>) {
            onMain {
                when (v.propertyId) {
                    VehiclePropertyIds.PERF_VEHICLE_SPEED -> {
                        mark("speed")
                        (v.value as? Float)?.let { speedKmh = abs(it) * 3.6f } // m/s → km/h
                    }
                    VehiclePropertyIds.PARKING_BRAKE_ON -> {
                        mark("parkingBrake")
                        parkingBrake = v.value as? Boolean
                    }
                }
            }
        }

        override fun onErrorEvent(propertyId: Int, zone: Int) {
            Log.w(TAG, "차량 속성 오류 id=$propertyId zone=$zone")
        }
    }

    private val tick = object : Runnable {
        override fun run() {
            callback?.invoke(snapshot())
            handler.postDelayed(this, TICK_MS)
        }
    }

    fun snapshot() = VehicleSnapshot(
        timeMs = System.currentTimeMillis(),
        gear = gear,
        speedKmh = speedKmh,
        steeringDeg = steeringDeg,
        turnSignal = turnSignal,
        brakePressed = brake,
        parkingBrakeOn = parkingBrake.takeUnless { "parkingBrake" in ignored },
        doorOpen = doorOpen,
        seatbeltOn = seatbelt.takeUnless { "seatbelt" in ignored },
    )

    override fun start(onSnapshot: (VehicleSnapshot) -> Unit) {
        callback = onSnapshot
        startVehicleSdk()
        startCarProperties()
        handler.removeCallbacks(tick)
        handler.post(tick)
    }

    override fun stop() {
        handler.removeCallbacks(tick)
        callback = null
        vehicle?.let { v ->
            runCatching {
                v.getGear().unregisterGearSelection(gearListener)
                v.getTurnSignal().unregisterTurnSignalState(turnListener)
                v.getSteeringWheel().unregisterSteeringWheelAngle(steeringListener)
                v.getBrake().unregisterBrakePedalStatus(brakeListener)
                v.getDoor().unregisterDoorPosition(driverDoor, doorListener)
                v.getSeat().unregisterSeatBeltBuckledState(driverSeat, beltListener)
            }
            runCatching { v.release() }
        }
        vehicle = null
        runCatching { props?.unregisterCallback(propCallback) }
        runCatching { car?.disconnect() }
        props = null
        car = null
    }

    private fun startVehicleSdk() {
        val v = runCatching { Vehicle(context).also { it.initialize() } }
            .onFailure { Log.w(TAG, "Vehicle SDK 초기화 실패", it) }
            .getOrNull() ?: return
        vehicle = v
        // 하나가 실패해도 나머지는 받도록 각각 감싼다
        runCatching { v.getGear().registerGearSelection(gearListener) }
        runCatching { v.getTurnSignal().registerTurnSignalState(turnListener) }
        runCatching { v.getSteeringWheel().registerSteeringWheelAngle(steeringListener) }
        runCatching { v.getBrake().registerBrakePedalStatus(brakeListener) }
        runCatching { v.getDoor().registerDoorPosition(driverDoor, doorListener) }
        runCatching { v.getSeat().registerSeatBeltBuckledState(driverSeat, beltListener) }
    }

    private fun startCarProperties() {
        val c = runCatching { Car.createCar(context) }
            .onFailure { Log.w(TAG, "Car 연결 실패", it) }
            .getOrNull() ?: return
        car = c
        val pm = c.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager ?: return
        props = pm
        runCatching {
            pm.registerCallback(propCallback, VehiclePropertyIds.PERF_VEHICLE_SPEED, CarPropertyManager.SENSOR_RATE_FASTEST)
        }.onFailure { Log.w(TAG, "속도 구독 실패", it) }
        runCatching {
            pm.registerCallback(propCallback, VehiclePropertyIds.PARKING_BRAKE_ON, CarPropertyManager.SENSOR_RATE_ONCHANGE)
        }.onFailure { Log.w(TAG, "주차 브레이크 구독 실패", it) }
    }

    companion object {
        private const val TAG = "PleosSignal"
        const val TICK_MS = 100L

        /** 매니페스트에 선언하고 실행 중 요청할 권한 */
        val PERMISSIONS = arrayOf(
            "pleos.car.permission.CAR_DRIVE",
            "pleos.car.permission.CAR_LIGHTS",
            "pleos.car.permission.CAR_DOORS",
            "pleos.car.permission.CAR_SEATS",
            "pleos.car.permission.CAR_SAFETY",
            Car.PERMISSION_SPEED,
            Car.PERMISSION_POWERTRAIN,
        )
    }
}
