package com.barrybecker4.simulations.traffic.viewer.adapter

import com.barrybecker4.simulations.traffic.simulation.RoadTopology
import org.graphstream.graph.implementations.MultiGraph

case class TrafficGraphBundle(
    graph: MultiGraph,
    intersectionSubGraphs: IndexedSeq[IntersectionSubGraph],
    topology: RoadTopology
)
