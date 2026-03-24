package com.barrybecker4.simulations.traffic.viewer

import com.barrybecker4.graph.visualization.render.GraphViewerPipe
import com.barrybecker4.simulations.traffic.demo.TrafficOrchestrator


/**
 * Hardcoded demo graph from [[TrafficGraphGenerator]] (not loaded via [[com.barrybecker4.simulations.traffic.graph.TrafficGraphParser]]).
 *
 * Intersection traffic signals and [[com.barrybecker4.simulations.traffic.viewer.adapter.IntersectionSubGraph]] updates are not run here
 * because this graph is not wired through [[com.barrybecker4.simulations.traffic.viewer.adapter.TrafficStreamAdapter]].
 * Vehicles still move along edges. For full simulation, open a map from [[TrafficViewerFrame]].
 */
object TrafficDemoApplication {
  private val SPRITE_COUNT = 100

  def main(args: Array[String]): Unit = {
    System.setProperty("org.graphstream.ui", "org.graphstream.ui.swing.util.Display")

    val graph = new TrafficGraphGenerator().generateGraph()
    val pipeIn = graph.display(false).newViewerPipe()
    val initialSpeed = 10.0
    new TrafficOrchestrator(graph, SPRITE_COUNT, initialSpeed, IndexedSeq.empty, pipeIn).run()
  }
}
