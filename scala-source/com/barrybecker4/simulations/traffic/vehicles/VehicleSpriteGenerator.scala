package com.barrybecker4.simulations.traffic.vehicles

import com.barrybecker4.simulations.traffic.simulation.{RoadTopology, SimVehicle, SimulationState, TrafficSimulationConfig}
import com.barrybecker4.simulations.traffic.vehicles.placement.VehiclePlacer
import org.graphstream.graph.Graph

class VehicleSpriteGenerator(private val numSprites: Int, initialSpeed: Double, val config: TrafficSimulationConfig) {

  private var spriteManager: VehicleSpriteManager = _

  def getSpriteManager: VehicleSpriteManager = spriteManager

  def addSprites(graph: Graph, topology: RoadTopology, state: SimulationState): Unit = {
    val simVehicles = (0 until numSprites).map { i =>
      val sv = new SimVehicle(s"$i", initialSpeed, config, scala.util.Random(i * 7919))
      state.registerVehicle(sv)
      sv
    }.toArray
    spriteManager = new VehicleSpriteManager(graph, state, topology, config)
    spriteManager.setSpriteFactory(new VehicleSpriteFactory(simVehicles))
    new VehiclePlacer(spriteManager, graph, topology, config, numSprites).placeVehicleSprites()
  }
}
