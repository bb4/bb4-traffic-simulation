package com.barrybecker4.simulations.traffic.vehicles

import com.barrybecker4.simulations.traffic.simulation.SimVehicle
import org.graphstream.ui.graphicGraph.stylesheet.Values
import org.graphstream.ui.spriteManager.{Sprite, SpriteFactory, SpriteManager}

class VehicleSpriteFactory(simVehicles: Array[SimVehicle]) extends SpriteFactory {

  override def newSprite(identifier: String, manager: SpriteManager, position: Values): Sprite =
    new VehicleSprite(identifier, simVehicles(identifier.toInt), manager.asInstanceOf[VehicleSpriteManager])
}
