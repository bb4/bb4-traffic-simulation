package com.barrybecker4.simulations.traffic.viewer.adapter

import com.barrybecker4.simulations.traffic.graph.model.Intersection
import com.barrybecker4.simulations.traffic.signals.TrafficSignal
import com.barrybecker4.simulations.traffic.simulation.{RoadTopology, SimulationState, TrafficSimulationConfig}
import com.barrybecker4.simulations.traffic.simulation.SimVehicle
import org.graphstream.graph.{Edge, Node}
import org.graphstream.graph.implementations.MultiGraph

/**
 * Represents the nodes and edges in an intersection.
 * Regulates the movement of vehicles on the edges leading into the intersection
 */
case class IntersectionSubGraph(intersection: Intersection, graph: MultiGraph, config: TrafficSimulationConfig) {

  private val signal: TrafficSignal = intersection.signalType.create(intersection.ports.size, config)
  private val builder = new IntersectionSubGraphBuilder(intersection, graph)

  def getIncomingNode(portId: Int): Node = builder.incomingNodes(portId)
  def getOutgoingNode(portId: Int): Node = builder.outgoingNodes(portId)

  /** Called by the orchestrator to update the intersection every timeStep
   * - Within an intersection, examine the sprites on intersection edges and the edges leading into the intersection.
   * - Sprites should be aware of how distant the next sprite in front is, if any.
   *     - There should be an optimal distance to it
   *     - If >= distantThreshold, don't try to catch up
   *     - If < distanceThreshold, and > optimalDistance, then try to speed up a little to get closer to optimal
   *     - If < optimalDistance, then break until >= optimalDistance
   *     - If Signal says to slow down, then brake to slow speed
   *     - If upcoming Signal is red, then start to smoothly slow so that we can be stopped by the time we get there
   * Under no circumstances should a vehicle be able to pass another.
   */
  def update(deltaTime: Double, state: SimulationState, topology: RoadTopology): Unit = {
    for (portId <- intersection.ports.indices) {
      val inNode: Node = getIncomingNode(portId)
      signal.showLight(inNode, portId)
      assert(inNode.getInDegree == 1, "There should be exactly one edge entering the intersection on a port")
      val incomingEdge: Edge = inNode.getEnteringEdge(0)
      updateVehiclesOnEdge(handleSignal = true, incomingEdge, portId, deltaTime, state, topology)
      for (j <- 0 until inNode.getOutDegree) {
        val outgoingEdge = inNode.getLeavingEdge(j)
        updateVehiclesOnEdge(handleSignal = false, outgoingEdge, portId, deltaTime, state, topology)
      }
    }
  }

  private def updateVehiclesOnEdge(
      handleSignal: Boolean,
      edge: Edge,
      portId: Int,
      deltaTime: Double,
      state: SimulationState,
      topology: RoadTopology
  ): Unit = {
    val edgeLen = topology.length(edge.getId)
    val sprites = state.getSimVehiclesOnEdge(edge.getId)

    val sortedSprites: IndexedSeq[SimVehicle] = sprites.toIndexedSeq.sortBy(_.getPosition)
    state.setLastVehicle(edge.getId, sortedSprites.headOption)
    if (handleSignal) {
      signal.handleTraffic(sortedSprites, portId, edgeLen, deltaTime, state)
    }

    if (sprites.nonEmpty) {
      val leadVehicle = sortedSprites.last
      val trailingOnNextStreet: Option[SimVehicle] =
        state.getLastVehicle(leadVehicle.getNextEdgeId)
      val endSize =
        if (trailingOnNextStreet.isEmpty) sortedSprites.size - 1 else sortedSprites.size
      for (i <- 0 until endSize) {
        val sprite = sortedSprites(i)
        val (nextSprite, nextPosition) =
          nextCarAndPosition(sortedSprites, i, trailingOnNextStreet)
        val distanceToNext = (nextPosition - sprite.getPosition) * edgeLen
        assert(distanceToNext > 0, "The distance to the car in front should never be less than 0")
        adjustSpeedTowardNext(sprite, nextSprite, distanceToNext)
      }
    }
  }

  private def nextCarAndPosition(
      sortedSprites: IndexedSeq[SimVehicle],
      i: Int,
      trailingOnNextStreet: Option[SimVehicle]
  ): (SimVehicle, Double) =
    if (i == sortedSprites.size - 1)
      (trailingOnNextStreet.get, 1.0 + trailingOnNextStreet.get.getPosition)
    else
      (sortedSprites(i + 1), sortedSprites(i + 1).getPosition)

  private def adjustSpeedTowardNext(sprite: SimVehicle, nextSprite: SimVehicle, distanceToNext: Double): Unit = {
    if (distanceToNext < signal.getFarDistance) {
      if (distanceToNext < signal.getOptimalDistance) {
        if (sprite.getSpeed >= nextSprite.getSpeed) {
          sprite.setSpeed(nextSprite.getSpeed * 0.9)
        }
      } else if (sprite.getSpeed <= nextSprite.getSpeed + 0.05) {
        sprite.setSpeed(nextSprite.getSpeed + 0.1)
      }
    } else {
      sprite.accelerate(0.05)
    }
  }
}
