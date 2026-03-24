package com.barrybecker4.simulations.traffic.simulation

import scala.util.Random

/**
 * Authoritative vehicle state for the simulation (independent of GraphStream sprites).
 */
class SimVehicle(
    val id: String,
    initialSpeed: Double,
    val config: TrafficSimulationConfig,
    val rnd: Random = new Random(0)
) {

  private var positionPct: Double = 0.0
  private var speed: Double = initialSpeed
  private var incrementalDistance: Double = 0
  private var totalDistance: Double = 0
  private var currentEdgeId: String = _
  private var nextEdgeId: String = _

  def getSpeed: Double = speed
  def getPosition: Double = positionPct
  def getCurrentEdgeId: String = currentEdgeId
  def getNextEdgeId: String = nextEdgeId

  def setCurrentEdgeId(edgeId: String): Unit = currentEdgeId = edgeId
  def setNextEdgeId(edgeId: String): Unit = nextEdgeId = edgeId

  def accelerate(acceleration: Double): Unit = {
    val maxA = config.maxAcceleration
    speed += math.max(-maxA, math.min(acceleration, maxA))
    speed = math.max(0, math.min(speed, config.preferredSpeed))
  }

  def setSpeed(newSpeed: Double): Unit = {
    speed = math.max(0, math.min(newSpeed, config.maxSpeed))
  }

  def brake(stoppingDistance: Double, deltaTime: Double): Unit = {
    if (stoppingDistance < 0.01) {
      stop()
    } else {
      val brake = math.min(config.maxAcceleration, speed * deltaTime / stoppingDistance)
      accelerate(-brake)
    }
  }

  def stop(): Unit = {
    speed = 0
  }

  def setPosition(pct: Double): Unit = {
    positionPct = pct
  }

  def getTotalDistance: Double = totalDistance
  def getIncrementalDistance: Double = incrementalDistance

  def resetIncrementalDistance(): Unit = incrementalDistance = 0

  def addDistanceAccounting(deltaTime: Double): Unit = {
    val dist = deltaTime * speed
    incrementalDistance += dist
    totalDistance += dist
  }

  def pickNextEdgeFromTarget(topology: RoadTopology, targetNodeId: String): Unit = {
    nextEdgeId = topology.randomOutgoingEdgeId(targetNodeId, rnd)
  }
}
