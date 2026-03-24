package com.barrybecker4.simulations.traffic.simulation

import scala.collection.mutable

/**
 * Authoritative per-edge vehicle sets and cached tail-of-queue references used by intersection logic.
 */
class SimulationState(val config: TrafficSimulationConfig, val topology: RoadTopology) {

  private val vehicles = mutable.Map.empty[String, SimVehicle]
  private val vehiclesByEdge = mutable.Map.empty[String, mutable.Set[SimVehicle]]
  private val lastVehicleCache = mutable.Map.empty[String, Option[SimVehicle]]

  def registerVehicle(v: SimVehicle): Unit = vehicles(v.id) = v

  def getVehicle(id: String): Option[SimVehicle] = vehicles.get(id)

  def allSimVehicles: Iterable[SimVehicle] = vehicles.values

  def addVehicleToEdge(edgeId: String, v: SimVehicle): Unit =
    vehiclesByEdge.getOrElseUpdate(edgeId, mutable.Set()) += v

  def removeVehicleFromEdge(edgeId: String, v: SimVehicle): Unit =
    vehiclesByEdge.get(edgeId).foreach(_ -= v)

  def getSimVehiclesOnEdge(edgeId: String): mutable.Set[SimVehicle] =
    vehiclesByEdge.getOrElseUpdate(edgeId, mutable.Set())

  def setLastVehicle(edgeId: String, last: Option[SimVehicle]): Unit =
    lastVehicleCache(edgeId) = last

  def getLastVehicle(edgeId: String): Option[SimVehicle] =
    lastVehicleCache.getOrElse(edgeId, None)
}
