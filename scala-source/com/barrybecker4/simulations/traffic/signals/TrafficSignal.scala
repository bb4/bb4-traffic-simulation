package com.barrybecker4.simulations.traffic.signals

import com.barrybecker4.simulations.traffic.signals.SignalState.{GREEN, RED, YELLOW}
import com.barrybecker4.simulations.traffic.simulation.{SimulationState, TrafficSimulationConfig}
import com.barrybecker4.simulations.traffic.simulation.SimVehicle
import org.graphstream.graph.Node

import java.util.concurrent.{Executors, ScheduledExecutorService}

trait TrafficSignal(numStreets: Int, val config: TrafficSimulationConfig) {

  protected val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
  protected var yellowStartTime = 0L

  def getOptimalDistance: Double = config.signalOptimalDistance
  def getFarDistance: Double = config.signalFarDistance
  def getYellowDurationSecs: Int = config.signalYellowDurationSecs
  def getGreenDurationSecs: Int = config.signalGreenDurationSecs
  def getLightState(port: Int): SignalState

  def handleTraffic(
      sortedVehicles: IndexedSeq[SimVehicle],
      portId: Int,
      edgeLen: Double,
      deltaTime: Double,
      state: SimulationState
  ): Unit

  def showLight(node: Node, portId: Int): Unit = {
    val lightState = getLightState(portId)
    node.setAttribute("ui.style", "size: 30px; z-index:0; fill-color: " + lightState.color)
  }

  def printLightStates(): Unit = {
    val states = Range(0, numStreets).map(i => getLightState(i))
    println(states)
  }

  protected def handleTrafficBasedOnLightState(
      sortedVehicles: IndexedSeq[SimVehicle],
      portId: Int,
      edgeLen: Double,
      deltaTime: Double
  ): Unit = {
    val lightState = getLightState(portId)
    if (sortedVehicles.isEmpty) return
    val vehicleClosestToLight = sortedVehicles.last

    lightState match {
      case RED =>
        if (vehicleClosestToLight.getSpeed > 0.0 && vehicleClosestToLight.getPosition > 0.97) {
          vehicleClosestToLight.stop()
        } else if (vehicleClosestToLight.getPosition > 0.9) {
          vehicleClosestToLight.setSpeed(vehicleClosestToLight.getSpeed * .9)
        }
      case YELLOW =>
        val yellowElapsedTime = (System.currentTimeMillis() - yellowStartTime) / 1000.0
        val yellowRemainingTime = getYellowDurationSecs.toDouble - yellowElapsedTime
        findVehicleAffectedByYellow(sortedVehicles, edgeLen, yellowRemainingTime).foreach { vehicle =>
          vehicle.brake(yellowRemainingTime * vehicle.getSpeed, deltaTime)
        }
      case GREEN =>
        vehicleClosestToLight.accelerate(0.1)
    }
  }

  /** Vehicle that is in the "commit or stop" band for yellow, if any. */
  private def findVehicleAffectedByYellow(
      sortedVehicles: IndexedSeq[SimVehicle],
      edgeLen: Double,
      yellowRemainingTime: Double
  ): Option[SimVehicle] = {
    var idx = sortedVehicles.size
    var found: Option[SimVehicle] = None
    while (found.isEmpty && idx > 0) {
      idx -= 1
      val vehicle = sortedVehicles(idx)
      val distanceToLight = (1.0 - vehicle.getPosition) * edgeLen
      val distAtCurrentSpeed = yellowRemainingTime * vehicle.getSpeed
      val farDist = (yellowRemainingTime + getYellowDurationSecs) * vehicle.getSpeed
      if (distAtCurrentSpeed > distanceToLight && distAtCurrentSpeed < farDist) {
        found = Some(vehicle)
      }
    }
    found
  }
}
