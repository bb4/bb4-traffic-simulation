package com.barrybecker4.simulations.traffic

import com.barrybecker4.simulations.traffic.viewer.TrafficViewerFrame


/**
 * Ideas:
 *  - Avoid gridlock. If cars are stopped in one of the outgoing intersection streets, then we need to turn red.
 *     - add a lastVehicle attribute to streets.
 *     - Use the lastVehicle in the traffic flow calculation.
 */
object TrafficApp extends App {

  /** Required before any GraphStream graph is built; selects Swing viewer implementation. */
  System.setProperty("org.graphstream.ui", "org.graphstream.ui.swing.util.Display")

  val frame = new TrafficViewerFrame()

}
