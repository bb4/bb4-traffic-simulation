package com.barrybecker4.simulations.traffic.simulation

/**
 * Centralized tunable parameters for the traffic simulation.
 */
case class TrafficSimulationConfig(
    deltaTimeSecs: Double = 0.02,
    /**
     * Multiplier for wall-clock sleep per tick: `sleepMs = (deltaTimeSecs * 1000 * realtimeSpeedFactor).toLong`.
     * `0` means no sleep (CPU-limited, fastest simulation).
     * Default `0.1` matches the original demo (`Thread.sleep(2)` with `deltaTimeSecs = 0.02` → 2ms per tick).
     */
    realtimeSpeedFactor: Double = 0.1,
    maxSpeed: Double = 20.0,
    preferredSpeed: Double = 16.0,
    maxAcceleration: Double = 4.0,
    placerMinGap: Double = 8.0,
    placerVehicleLength: Double = 20.0,
    signalOptimalDistance: Double = 30.0,
    signalFarDistance: Double = 200.0,
    signalYellowDurationSecs: Int = 2,
    signalGreenDurationSecs: Int = 4,
    semaphoreGreenDurationSecs: Int = 8
) {
  def preferredSpeedFromMax: TrafficSimulationConfig =
    copy(preferredSpeed = 0.8 * maxSpeed)
}

object TrafficSimulationConfig {
  val Default: TrafficSimulationConfig = TrafficSimulationConfig()
}
