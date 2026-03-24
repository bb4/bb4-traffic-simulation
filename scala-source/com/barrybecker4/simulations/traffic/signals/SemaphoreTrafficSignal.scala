package com.barrybecker4.simulations.traffic.signals

import com.barrybecker4.simulations.traffic.signals.SignalState
import com.barrybecker4.simulations.traffic.signals.SignalState.*
import com.barrybecker4.simulations.traffic.vehicles.VehicleSprite

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
class SemaphoreTrafficSignal(numStreets: Int) extends TrafficSignal(numStreets) {
  import SemaphoreTrafficSignal.NoStreetHoldsSemaphore

  private val lightStates: Array[SignalState] = Array.fill(numStreets)(RED)
  private var currentSchedule: ScheduledFuture[?] = _
  private var streetWithSemaphore: Int = NoStreetHoldsSemaphore
  private var lastToBecomeRed: Int = -1

  override def getGreenDurationSecs: Int = 8
  override def getLightState(street: Int): SignalState = lightStates(street)

  def shutdown(): Unit = scheduler.shutdown()

  def handleTraffic(sortedVehicles: IndexedSeq[VehicleSprite],
                    portId: Int, edgeLen: Double, deltaTime: Double): Unit = {
    handleTrafficBasedOnLightState(sortedVehicles, portId, edgeLen, deltaTime)
    updateSemaphore(sortedVehicles, portId, edgeLen)
  }

  private def updateSemaphore(sortedVehicles: IndexedSeq[VehicleSprite],
                              portId: Int, edgeLen: Double): Unit = {
    val lightState = getLightState(portId)
    streetWithSemaphore match {
      case NoStreetHoldsSemaphore =>
        assert(lightState == RED, "The light state was unexpectedly " + lightState)
        trySwitchingToGreen(portId, sortedVehicles, edgeLen)
      case `portId` =>
        assert(lightState != RED, "The light state was unexpectedly " + lightState)
        whenHoldingSemaphore(portId, sortedVehicles, edgeLen, lightState)
      case _ =>
        ()
    }
  }

  private def whenHoldingSemaphore(
      portId: Int,
      sortedVehicles: IndexedSeq[VehicleSprite],
      edgeLen: Double,
      lightState: SignalState
  ): Unit = {
    if (areCarsComing(sortedVehicles, edgeLen)) {
      val lastCar = sortedVehicles.last
      if (isNextStreetJammed(lastCar) && lightState == GREEN) {
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

  private def isNextStreetJammed(lastCar: VehicleSprite): Boolean = {
    val lastIntersectionCar = lastCar.getNextEdge.getAttribute("lastVehicle", classOf[Option[VehicleSprite]])
    if (lastIntersectionCar.nonEmpty && lastIntersectionCar.get.getSpeed < 0.1) {
      true
    } else {
      val nextNode = lastCar.getNextEdge.getTargetNode
      assert(nextNode.getOutDegree == 1)
      val nextStreet = nextNode.getLeavingEdge(0)
      val len = nextStreet.getAttribute("length", classOf[Object]).asInstanceOf[Double]
      val lastVehicleOnNextStreet =
        nextStreet.getAttribute("lastVehicle", classOf[Option[VehicleSprite]])
      lastVehicleOnNextStreet.exists { v =>
        v.getSpeed < 0.1 && v.getPosition * len < getFarDistance
      }
    }
  }

  private def trySwitchingToGreen(street: Int, sortedVehicles: IndexedSeq[VehicleSprite],
                            edgeLen: Double): Unit = {
    assert(lightStates(street) == RED)
    assert(streetWithSemaphore == NoStreetHoldsSemaphore, "semaphore was not available. It was " + streetWithSemaphore)
    if (lastToBecomeRed != street) {
      if (areCarsComing(sortedVehicles, edgeLen) && !isNextStreetJammed(sortedVehicles.last)) {
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

  private def switchToYellow(street: Int, sortedVehicles: IndexedSeq[VehicleSprite],
                             edgeLen: Double): Unit = {
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

  private def areCarsComing(sortedVehicles: IndexedSeq[VehicleSprite], edgeLen: Double): Boolean =
    sortedVehicles.nonEmpty
}


object SemaphoreTrafficSignal {
  /** Sentinel: no incoming street currently holds the green/yellow phase. */
  private[signals] val NoStreetHoldsSemaphore: Int = -1
}
