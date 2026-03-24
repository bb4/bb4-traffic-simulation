package com.barrybecker4.simulations.traffic.signals

import com.barrybecker4.simulations.traffic.signals.SignalState.*
import com.barrybecker4.simulations.traffic.simulation.{SimulationState, TrafficSimulationConfig}
import com.barrybecker4.simulations.traffic.simulation.SimVehicle

import java.util.concurrent.TimeUnit

/**
 * Only one street is allowed to proceed at once (on green).
 * That should prevent the possibility of accidents.
 * @param numStreets the number of streets leading into the intersection
 */
class DumbTrafficSignal(numStreets: Int, config: TrafficSimulationConfig) extends TrafficSignal(numStreets, config) {
  private var currentStreet: Int = 0
  private var lightState: SignalState = RED

  setInitialState()

  override def getLightState(street: Int): SignalState = {
    if (street == currentStreet) lightState else RED
  }

  def shutdown(): Unit = scheduler.shutdown()

  override def handleTraffic(
      sortedVehicles: IndexedSeq[SimVehicle],
      portId: Int,
      edgeLen: Double,
      deltaTime: Double,
      state: SimulationState
  ): Unit = {
    if (sortedVehicles.isEmpty) return
    val lightState = getLightState(portId)
    val vehicleClosestToLight = sortedVehicles.last

    lightState match {
      case RED =>
        if (vehicleClosestToLight.getSpeed > 0.0 && vehicleClosestToLight.getPosition > 0.96) {
          vehicleClosestToLight.stop()
        } else if (vehicleClosestToLight.getPosition > 0.85) {
          vehicleClosestToLight.setSpeed(vehicleClosestToLight.getSpeed * .98)
        }
      case YELLOW =>
        val yellowTime = getYellowDurationSecs.toDouble
        var vehicleIdx = sortedVehicles.size - 1
        var vehicle = vehicleClosestToLight
        while ((1.0 - vehicle.getPosition) * edgeLen < yellowTime * vehicle.getSpeed && vehicleIdx > 0) {
          vehicle.brake(yellowTime * vehicle.getSpeed * 0.8, deltaTime)
          vehicleIdx -= 1
          vehicle = sortedVehicles(vehicleIdx)
        }
      case GREEN =>
        vehicleClosestToLight.accelerate(0.01)
    }
  }

  def getRedDurationSecs: Int = (numStreets - 1) * (getGreenDurationSecs + getYellowDurationSecs)

  // Function to initialize the traffic light state and scheduling
  private def setInitialState(): Unit = switchToGreen()

  // Function to switch the light to green
  private def switchToGreen(): Unit = {
    lightState = GREEN
    scheduler.schedule(new Runnable {
      def run(): Unit = switchToYellow()
    }, getGreenDurationSecs, TimeUnit.SECONDS)
  }

  // Function to switch the light to yellow
  private def switchToYellow(): Unit = {
    lightState = YELLOW
    yellowStartTime = System.currentTimeMillis()
    scheduler.schedule(new Runnable {
      def run(): Unit = switchToRed()
    }, getYellowDurationSecs, TimeUnit.SECONDS)
  }

  private def switchToRed(): Unit = {
    lightState = RED
    currentStreet = (currentStreet + 1) % numStreets
    switchToGreen()
  }
}
