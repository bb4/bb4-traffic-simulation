package com.barrybecker4.simulations.traffic.signals

import com.barrybecker4.simulations.traffic.signals.SignalState.*
import com.barrybecker4.simulations.traffic.simulation.{SimulationState, TrafficSimulationConfig}
import com.barrybecker4.simulations.traffic.simulation.SimVehicle

import java.util.concurrent.{ScheduledFuture, TimeUnit}

/**
 * A more intelligent traffic light system that uses a semaphore to control the traffic lights.
 *  - If a car is within double yellow distance, then try to take semaphore and become green.
 *    Others remain red.
 *  - A light stays green until no cars within double yellow empty or time exceeded.
 *    If time exceeded, then turn yellow, else straight to red.
 *  - The next street (i.e. intersection node port) with cars waiting gets the semaphore and turns green.
 *  - If no cars coming, all will be red, and semaphore up for grabs.
 *
 * @param numStreets the number of streets leading into the intersection
 */
class SemaphoreTrafficSignal(numStreets: Int, config: TrafficSimulationConfig) extends TrafficSignal(numStreets, config) {
  import SemaphoreTrafficSignal.NoStreetHoldsSemaphore

  override def getGreenDurationSecs: Int = config.semaphoreGreenDurationSecs

  private val lightStates: Array[SignalState] = Array.fill(numStreets)(RED)
  private var currentSchedule: ScheduledFuture[?] = _
  private var streetWithSemaphore: Int = NoStreetHoldsSemaphore
  private var lastToBecomeRed: Int = -1

  def shutdown(): Unit = scheduler.shutdown()

  override def getLightState(street: Int): SignalState = lightStates(street)

  override def handleTraffic(
      sortedVehicles: IndexedSeq[SimVehicle],
      portId: Int,
      edgeLen: Double,
      deltaTime: Double,
      state: SimulationState
  ): Unit = {
    handleTrafficBasedOnLightState(sortedVehicles, portId, edgeLen, deltaTime)
    updateSemaphore(sortedVehicles, portId, edgeLen, state)
  }

  private def updateSemaphore(
      sortedVehicles: IndexedSeq[SimVehicle],
      portId: Int,
      edgeLen: Double,
      state: SimulationState
  ): Unit = {
    val lightState = getLightState(portId)
    streetWithSemaphore match {
      case NoStreetHoldsSemaphore =>
        assert(lightState == RED, "The light state was unexpectedly " + lightState)
        trySwitchingToGreen(portId, sortedVehicles, edgeLen, state)
      case `portId` =>
        assert(lightState != RED, "The light state was unexpectedly " + lightState)
        whenHoldingSemaphore(portId, sortedVehicles, edgeLen, lightState, state)
      case _ =>
        ()
    }
  }

  private def whenHoldingSemaphore(
      portId: Int,
      sortedVehicles: IndexedSeq[SimVehicle],
      edgeLen: Double,
      lightState: SignalState,
      state: SimulationState
  ): Unit = {
    if (areCarsComing(sortedVehicles, edgeLen)) {
      val lastCar = sortedVehicles.last
      if (isNextStreetJammed(lastCar, state) && lightState == GREEN) {
        println("Next street is jammed, so switching to yellow")
        switchToYellow(portId, sortedVehicles, edgeLen)
      }
    } else if (currentSchedule != null && lightState == GREEN) {
      println("No cars coming on street " + portId + " so canceling schedule and switching to red")
      currentSchedule.cancel(true)
      currentSchedule = null
      switchToRed(portId)
    }
  }

  private def isNextStreetJammed(lastCar: SimVehicle, state: SimulationState): Boolean = {
    val topology = state.topology
    val nextEdgeId = lastCar.getNextEdgeId
    val lastIntersectionCar = state.getLastVehicle(nextEdgeId)
    if (lastIntersectionCar.nonEmpty && lastIntersectionCar.get.getSpeed < 0.1) {
      true
    } else {
      val nextNodeId = topology.edge(nextEdgeId).targetNodeId
      topology.singleOutgoingEdgeId(nextNodeId) match {
        case Some(nextStreetId) =>
          val len = topology.length(nextStreetId)
          val lastVehicleOnNextStreet = state.getLastVehicle(nextStreetId)
          lastVehicleOnNextStreet.exists { v =>
            v.getSpeed < 0.1 && v.getPosition * len < getFarDistance
          }
        case None =>
          false
      }
    }
  }

  private def trySwitchingToGreen(street: Int, sortedVehicles: IndexedSeq[SimVehicle], edgeLen: Double, state: SimulationState): Unit = {
    assert(lightStates(street) == RED)
    assert(streetWithSemaphore == NoStreetHoldsSemaphore, "semaphore was not available. It was " + streetWithSemaphore)
    if (lastToBecomeRed != street) {
      if (areCarsComing(sortedVehicles, edgeLen) && !isNextStreetJammed(sortedVehicles.last, state)) {
        lightStates(street) = GREEN
        streetWithSemaphore = street
        sortedVehicles.last.accelerate(0.1)
        currentSchedule = scheduler.schedule(new Runnable {
          def run(): Unit = switchToYellow(street, sortedVehicles, edgeLen)
        }, getGreenDurationSecs, TimeUnit.SECONDS)
      } else {
        lastToBecomeRed = -1
      }
    }
  }

  private def switchToYellow(street: Int, sortedVehicles: IndexedSeq[SimVehicle], edgeLen: Double): Unit = {
    assert(lightStates(street) == GREEN)
    lightStates(street) = YELLOW
    yellowStartTime = System.currentTimeMillis()
    assert(streetWithSemaphore == street)
    println("switched to yellow and scheduling switch to red for street " + street + " schedule=" + currentSchedule)
    currentSchedule.cancel(true)
    currentSchedule = scheduler.schedule(new Runnable {
      def run(): Unit = switchToRed(street)
    }, getYellowDurationSecs, TimeUnit.SECONDS)
  }

  private def switchToRed(street: Int): Unit = {
    if (lightStates(street) == YELLOW) {
      println("switching to red from yellow on street " + street)
    }
    lightStates(street) = RED
    assert(streetWithSemaphore == street)
    streetWithSemaphore = NoStreetHoldsSemaphore
    lastToBecomeRed = street
  }

  private def areCarsComing(sortedVehicles: IndexedSeq[SimVehicle], edgeLen: Double): Boolean =
    sortedVehicles.nonEmpty
}

object SemaphoreTrafficSignal {
  /** Sentinel: no incoming street currently holds the green/yellow phase. */
  private[signals] val NoStreetHoldsSemaphore: Int = -1
}
