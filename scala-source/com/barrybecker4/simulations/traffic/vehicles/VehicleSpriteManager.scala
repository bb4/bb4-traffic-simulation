package com.barrybecker4.simulations.traffic.vehicles

import com.barrybecker4.simulations.traffic.simulation.{RoadTopology, SimulationState, TrafficSimulationConfig}
import org.graphstream.graph.{Edge, Graph}
import org.graphstream.ui.spriteManager.SpriteManager

import scala.collection.mutable

class VehicleSpriteManager(
    graph: Graph,
    val state: SimulationState,
    val topology: RoadTopology,
    val config: TrafficSimulationConfig
) extends SpriteManager(graph) {

  def getVehiclesOnEdge(edgeId: String): mutable.Set[VehicleSprite] = {
    val result = mutable.Set.empty[VehicleSprite]
    state.getSimVehiclesOnEdge(edgeId).foreach { sv =>
      result += getSprite(sv.id).asInstanceOf[VehicleSprite]
    }
    result
  }

  def addVehicleToEdge(edgeId: String, vehicleSprite: VehicleSprite): Unit = ()

  def getEdge(edgeId: String): Edge = graph.getEdge(edgeId)

  def removeVehicleFromEdge(edgeId: String, vehicleSprite: VehicleSprite): Unit = ()
}
