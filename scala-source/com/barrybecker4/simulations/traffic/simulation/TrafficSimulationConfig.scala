package com.barrybecker4.simulations.traffic.simulation

/**
 * Centralized tunable parameters for the traffic simulation.
 */
case class TrafficSimulationConfig(
    deltaTimeSecs: Double = 0.02,
    /** Multiplier for wall-clock sleep per tick; 0 means no sleep (run as fast as possible). */
    realtimeSpeedFactor: Double = 1.0,
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
