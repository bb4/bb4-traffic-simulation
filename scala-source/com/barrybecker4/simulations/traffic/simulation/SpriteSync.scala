package com.barrybecker4.simulations.traffic.simulation

import com.barrybecker4.simulations.traffic.vehicles.{VehicleSprite, VehicleSpriteManager}
import org.graphstream.ui.spriteManager.Sprite

import scala.jdk.CollectionConverters.*

/**
 * Pushes authoritative [[SimVehicle]] state into GraphStream sprites (single-writer sim thread).
 */
object SpriteSync {

  def syncAll(spriteManager: VehicleSpriteManager): Unit =
    spriteManager.forEach((s: Sprite) => syncOne(s.asInstanceOf[VehicleSprite]))

  private def syncOne(sprite: VehicleSprite): Unit = {
    val sv = sprite.simVehicle
    if (sv.getCurrentEdgeId == null) return
    val edgeId = sv.getCurrentEdgeId
    if (sprite.getAttachment == null || sprite.getAttachment.getId != edgeId) {
      sprite.attachToEdge(edgeId)
    }
    sprite.syncPositionFromState()
  }
}
