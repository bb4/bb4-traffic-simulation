package com.barrybecker4.simulations.traffic.viewer

import com.barrybecker4.simulations.traffic.demo.TrafficSimulationBootstrap
import com.barrybecker4.simulations.traffic.roadnet.TrafficGraphParser
import com.barrybecker4.simulations.traffic.simulation.TrafficSimulationConfig
import com.barrybecker4.simulations.traffic.viewer.adapter.TrafficStreamAdapter

import java.io.File
import scala.io.Source

/**
 * Headless-style demo: loads a default map from test data and runs the full simulation pipeline
 * (same as opening a file in [[TrafficViewerFrame]]).
 */
object TrafficDemoApplication {
  private val SPRITE_COUNT = 100
  private val DefaultMapName = "dumbTrafficMap1"
  private val DataPrefix = "scala-test/com/barrybecker4/simulations/traffic/data/"
  private val Suffix = ".txt"

  def main(args: Array[String]): Unit = {
    System.setProperty("org.graphstream.ui", "org.graphstream.ui.swing.util.Display")

    val config = TrafficSimulationConfig.Default
    val parser = TrafficGraphParser()
    val file = new File(DataPrefix + DefaultMapName + Suffix)
    val source = Source.fromFile(file.getAbsolutePath)
    val trafficGraph = parser.parseFromSource(source, DefaultMapName + Suffix)
    val adapter = TrafficStreamAdapter(trafficGraph, config)
    val bundle = adapter.build()
    val state = TrafficSimulationBootstrap.createState(config, bundle)
    val spriteGen = TrafficSimulationBootstrap.addSprites(bundle, state, SPRITE_COUNT, initialSpeed = 10.0, config)
    val pipeIn = bundle.graph.display(false).newViewerPipe()
    TrafficSimulationBootstrap.runOrchestrator(bundle, config, spriteGen, pipeIn)
  }
}
