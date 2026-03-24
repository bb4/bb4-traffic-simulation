package com.barrybecker4.simulations.traffic.viewer.adapter

import com.barrybecker4.simulations.traffic.graph.TrafficGraph
import com.barrybecker4.simulations.traffic.viewer.GraphStreamStyle
import com.barrybecker4.simulations.traffic.viewer.TrafficGraphUtil.{addEdgeLengths, showNodeLabels}
import org.graphstream.graph.implementations.MultiGraph

object TrafficStreamAdapter {
  val LARGE_GRAPH_THRESH = 60
  val COMPUTE_CURVES = false
  val SHOW_LABELS = false
}

/** Creates a stream graph from TrafficGraph
 */
case class TrafficStreamAdapter(trafficGraph: TrafficGraph) {

  var intersectionSubGraphs: IndexedSeq[IntersectionSubGraph] = _

  def createGraph(): MultiGraph = {
    val graph = new MultiGraph("Some traffic graph")

    intersectionSubGraphs = addIntersectionsToGraph(graph)
    addStreetsToGraph(graph)
    addEdgeLengths(graph)
    if (TrafficStreamAdapter.SHOW_LABELS)
      showNodeLabels(graph)

    graph.setAttribute("ui.stylesheet", GraphStreamStyle.loadStyleSheet())
    graph.setAttribute("ui.antialias", true)
    graph
  }

  private def addIntersectionsToGraph(graph: MultiGraph): IndexedSeq[IntersectionSubGraph] = {
    for {
      intersectionId <- 0 until trafficGraph.numIntersections
      intersection = trafficGraph.getIntersection(intersectionId)
    } yield IntersectionSubGraph(intersection, graph)
  }

  private def addStreetsToGraph(graph: MultiGraph): Unit =
    trafficGraph.streets.foreach { street =>
      StreetSubGraph(
        street,
        intersectionSubGraphs(street.intersectionIdx1),
        intersectionSubGraphs(street.intersectionIdx2),
        graph
      )
    }
}
