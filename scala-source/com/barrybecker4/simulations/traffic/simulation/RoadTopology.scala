package com.barrybecker4.simulations.traffic.simulation

import com.barrybecker4.simulations.traffic.viewer.adapter.IntersectionSubGraphBuilder
import com.barrybecker4.simulations.traffic.viewer.adapter.StreetSubGraph
import org.graphstream.graph.implementations.MultiGraph

import scala.collection.mutable
import scala.jdk.CollectionConverters.*

case class EdgeRecord(
    id: String,
    length: Double,
    edgeType: String,
    sourceNodeId: String,
    targetNodeId: String
) {
  def isStreet: Boolean = edgeType == StreetSubGraph.STREET_TYPE
  def isIntersection: Boolean = edgeType == IntersectionSubGraphBuilder.INTERSECTION_TYPE
}

/**
 * Static routing and geometry derived from the GraphStream graph after it is built.
 */
case class RoadTopology(
    private val edges: Map[String, EdgeRecord],
    private val outgoing: Map[String, IndexedSeq[String]]
) {

  def edge(edgeId: String): EdgeRecord = edges(edgeId)

  def length(edgeId: String): Double = edges(edgeId).length

  def outgoingEdgeIds(nodeId: String): IndexedSeq[String] =
    outgoing.getOrElse(nodeId, IndexedSeq.empty)

  def randomOutgoingEdgeId(nodeId: String, rnd: scala.util.Random): String = {
    val outs = outgoingEdgeIds(nodeId)
    require(outs.nonEmpty, s"no outgoing edges from node $nodeId")
    outs(rnd.nextInt(outs.size))
  }

  /** When a node has exactly one outgoing edge (typical when leaving an intersection onto a street). */
  def singleOutgoingEdgeId(nodeId: String): Option[String] =
    outgoingEdgeIds(nodeId) match {
      case seq if seq.size == 1 => Some(seq.head)
      case _                    => None
    }
}

object RoadTopology {

  def fromGraph(graph: MultiGraph): RoadTopology = {
    val edges = mutable.Map.empty[String, EdgeRecord]
    val outgoing = mutable.Map.empty[String, mutable.ArrayBuffer[String]]

    graph.edges().iterator().asScala.foreach { e =>
      val id = e.getId
      val len = e.getAttribute("length", classOf[Object]).asInstanceOf[Double]
      val t = e.getAttribute("type", classOf[Object]).asInstanceOf[String]
      val src = e.getSourceNode.getId
      val tgt = e.getTargetNode.getId
      edges(id) = EdgeRecord(id, len, t, src, tgt)
      outgoing.getOrElseUpdate(src, mutable.ArrayBuffer()) += id
    }

    RoadTopology(
      edges.toMap,
      outgoing.map { case (k, v) => k -> v.toIndexedSeq }.toMap
    )
  }
}
