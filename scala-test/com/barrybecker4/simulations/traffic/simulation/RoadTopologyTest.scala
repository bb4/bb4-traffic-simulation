package com.barrybecker4.simulations.traffic.simulation

import com.barrybecker4.simulations.traffic.viewer.adapter.StreetSubGraph
import org.graphstream.graph.implementations.MultiGraph
import org.junit.jupiter.api.Assertions.{assertEquals, assertTrue}
import org.junit.jupiter.api.Test

/**
 * Pure tests for [[RoadTopology]] (regression: adjacency must persist per source node).
 */
class RoadTopologyTest {

  @Test
  def fromGraphRecordsOutgoingPerSourceNode(): Unit = {
    val g = new MultiGraph("test")
    g.addNode("a")
    g.addNode("b")
    g.addNode("c")
    val e1 = g.addEdge("e1", "a", "b", true)
    e1.setAttribute("length", java.lang.Double.valueOf(10.0))
    e1.setAttribute("type", StreetSubGraph.STREET_TYPE)
    val e2 = g.addEdge("e2", "a", "c", true)
    e2.setAttribute("length", java.lang.Double.valueOf(5.0))
    e2.setAttribute("type", StreetSubGraph.STREET_TYPE)

    val topo = RoadTopology.fromGraph(g)
    val outA = topo.outgoingEdgeIds("a").sorted
    assertEquals(IndexedSeq("e1", "e2"), outA)
    assertTrue(topo.outgoingEdgeIds("b").isEmpty)
    assertTrue(topo.outgoingEdgeIds("c").isEmpty)
  }
}
