package com.barrybecker4.simulations.traffic.demo

import com.barrybecker4.simulations.traffic.roadnet.TrafficGraph
import com.barrybecker4.simulations.traffic.simulation.{SimulationState, TrafficSimulationConfig}
import com.barrybecker4.simulations.traffic.vehicles.VehicleSpriteGenerator
import com.barrybecker4.simulations.traffic.viewer.adapter.TrafficGraphBundle
import org.graphstream.ui.view.ViewerPipe

/**
 * Shared wiring: build [[SimulationState]], place sprites, and run [[TrafficOrchestrator]].
 */
object TrafficSimulationBootstrap {

  def createState(config: TrafficSimulationConfig, bundle: TrafficGraphBundle): SimulationState =
    new SimulationState(config, bundle.topology)

  def addSprites(
      bundle: TrafficGraphBundle,
      state: SimulationState,
      numVehicles: Int,
      initialSpeed: Double,
      config: TrafficSimulationConfig
  ): VehicleSpriteGenerator = {
    val gen = new VehicleSpriteGenerator(numVehicles, initialSpeed, config)
    gen.addSprites(bundle.graph, bundle.topology, state)
    gen
  }

  def runOrchestrator(
      bundle: TrafficGraphBundle,
      config: TrafficSimulationConfig,
      spriteGenerator: VehicleSpriteGenerator,
      viewerPipe: ViewerPipe
  ): Unit =
    new TrafficOrchestrator(config, bundle.intersectionSubGraphs, viewerPipe, spriteGenerator).run()
}
