package com.barrybecker4.simulations.traffic.simulation

/**
 * Integrates vehicle motion along edges and handles transitions between edges using [[RoadTopology]].
 */
object VehicleSimulationEngine {

  def attachVehicleToEdge(v: SimVehicle, edgeId: String, topology: RoadTopology, state: SimulationState): Unit = {
    if (v.getCurrentEdgeId != null) {
      state.removeVehicleFromEdge(v.getCurrentEdgeId, v)
    }
    v.setCurrentEdgeId(edgeId)
    state.addVehicleToEdge(edgeId, v)
    v.pickNextEdgeFromTarget(topology, topology.edge(edgeId).targetNodeId)
  }

  def moveAll(vehicles: Iterable[SimVehicle], deltaTime: Double, topology: RoadTopology, state: SimulationState): Unit =
    vehicles.foreach(moveOne(_, deltaTime, topology, state))

  private def moveOne(v: SimVehicle, deltaTime: Double, topology: RoadTopology, state: SimulationState): Unit = {
    val edgeId = v.getCurrentEdgeId
    if (edgeId == null) return
    val edgeLen = topology.length(edgeId)
    var p = v.getPosition
    val step = deltaTime * v.getSpeed / edgeLen
    v.addDistanceAccounting(deltaTime)
    p += step
    if (p < 0 || p > 1)
      advanceToNextEdge(v, p, step, topology, state)
    else
      v.setPosition(p)
  }

  private def advanceToNextEdge(v: SimVehicle, p: Double, step: Double, topology: RoadTopology, state: SimulationState): Unit = {
    val edgeId = v.getCurrentEdgeId
    val edge = topology.edge(edgeId)
    val nodeId = if (step > 0) edge.targetNodeId else edge.sourceNodeId
    val offset = math.abs(p % 1)
    val nextId = v.getNextEdgeId
    val nextRec = topology.edge(nextId)
    val pos = if (nodeId == nextRec.sourceNodeId) offset else 1.0 - offset

    state.removeVehicleFromEdge(edgeId, v)
    v.setCurrentEdgeId(nextId)
    state.addVehicleToEdge(nextId, v)
    v.pickNextEdgeFromTarget(topology, nextRec.targetNodeId)
    v.setPosition(pos)
  }
}
