package com.barrybecker4.simulations.traffic.demo

import org.graphstream.ui.view.ViewerPipe
import com.barrybecker4.graph.visualization.render.GraphViewerPipe
import com.barrybecker4.simulations.traffic.simulation.{SpriteSync, TrafficSimulationConfig, VehicleSimulationEngine}
import com.barrybecker4.simulations.traffic.vehicles.VehicleSpriteGenerator
import com.barrybecker4.simulations.traffic.viewer.TrafficGraphUtil.sleep
import com.barrybecker4.simulations.traffic.viewer.adapter.IntersectionSubGraph

class TrafficOrchestrator(
    config: TrafficSimulationConfig,
    intersectionSubGraphs: IndexedSeq[IntersectionSubGraph],
    viewerPipe: ViewerPipe,
    spriteGenerator: VehicleSpriteGenerator
) {
  final private val viewerListener = new ViewerAdapter

  def run(): Unit = {
    val pipeIn = new GraphViewerPipe("my pipe", viewerPipe)
    pipeIn.addViewerListener(viewerListener)

    try {
      simulateTrafficFlow(pipeIn)
    } catch {
      case e: Exception => throw new IllegalStateException(e)
    }
  }

  private def simulateTrafficFlow(pipeIn: ViewerPipe): Unit = {
    val state = spriteGenerator.getSpriteManager.state
    val topology = state.topology
    val delta = config.deltaTimeSecs
    while (viewerListener.isLooping) {
      intersectionSubGraphs.foreach(_.update(delta, state, topology))
      VehicleSimulationEngine.moveAll(state.allSimVehicles, delta, topology, state)
      SpriteSync.syncAll(spriteGenerator.getSpriteManager)
      val sleepMs = (delta * 1000.0 * config.realtimeSpeedFactor).toLong
      if (sleepMs > 0) Thread.sleep(sleepMs)
    }
  }
}
