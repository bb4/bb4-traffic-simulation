package com.barrybecker4.simulations.traffic.vehicles

import com.barrybecker4.simulations.traffic.simulation.{SimVehicle, VehicleSimulationEngine}
import com.barrybecker4.simulations.traffic.vehicles.VehicleSprite.DEBUG
import org.graphstream.graph.Edge
import org.graphstream.ui.spriteManager.Sprite

object VehicleSprite {
  private val DEBUG = false
}

/**
 * GraphStream sprite view of a [[com.barrybecker4.simulations.traffic.simulation.SimVehicle]].
 * Motion and per-edge state are driven by the simulation engine; this class syncs attachment and position.
 */
class VehicleSprite(
    identifier: String,
    val simVehicle: SimVehicle,
    manager: VehicleSpriteManager
) extends Sprite(identifier, manager) {

  def getSpeed: Double = simVehicle.getSpeed
  def getNextEdge: Edge = manager.getEdge(simVehicle.getNextEdgeId)

  override def attachToEdge(edgeId: String): Unit = {
    val edge = manager.getEdge(edgeId)
    if (this.attachment != edge) {
      this.detach()
      this.attachment = edge
      VehicleSimulationEngine.attachVehicleToEdge(simVehicle, edgeId, manager.topology, manager.state)
    }
    this.attachment.setAttribute(this.completeId, Array(0: java.lang.Double))
  }

  override def detach(): Unit = {
    if (getAttachment != null && getAttachment.isInstanceOf[Edge]) {
      val edge = getAttachment.asInstanceOf[Edge]
      manager.state.removeVehicleFromEdge(edge.getId, simVehicle)
      super.detach()
    }
  }

  /** Used when placing vehicles; updates both model and GraphStream. */
  override def setPosition(pct: Double): Unit = {
    if (simVehicle.getPosition != pct) {
      simVehicle.setPosition(pct)
      super.setPosition(pct)
    }
  }

  /** Copies model position to the sprite without mutating the `simVehicle` model (already updated by the engine). */
  def syncPositionFromState(): Unit = {
    super.setPosition(simVehicle.getPosition)
  }

  def getPosition: Double = simVehicle.getPosition
  def getTotalDistance: Double = simVehicle.getTotalDistance
  def getIncrementalDistance: Double = simVehicle.getIncrementalDistance
  def resetIncrementalDistance(): Unit = simVehicle.resetIncrementalDistance()

  def applyDebugLabel(): Unit = {
    val edgeId =
      if (getAttachment != null && getAttachment.isInstanceOf[Edge]) getAttachment.asInstanceOf[Edge].getId
      else ""
    if (DEBUG && (this.getId == "60" || edgeId == "i2:p0-i1:p0"))
      setAttribute("ui.label", s"id: ${getId} pct: ${simVehicle.getPosition.toFloat}        s: ${simVehicle.getSpeed.toFloat} edge:$edgeId")
    else
      setAttribute("ui.label", "")
  }
}
