package com.barrybecker4.simulations.traffic.roadnet

import com.barrybecker4.common.geometry.FloatLocation
import com.barrybecker4.simulations.traffic.roadnet.model.{Intersection, Port, Street}
import com.barrybecker4.simulations.traffic.signals.TrafficSignalType


/**
 * Parse traffic graphs. The format is as follows
 *
 * nIntersections nStreets numVehicles
 * x_0 y_0 signalType angle_0a, radialLength_0a angle_0b, radialLength_0b …
 * x_1 y_1 signalType angle_1a, radialLength_1a angle_1b, radialLength_1b …
 * :
 * x_n y_n signalType …
 * intersectionId_i1 port_i intersectionId_j1 port_j
 * :
 * intersectionId_in port_i intersectionId_jn port_j
 */
case class TrafficGraphParser() {

  def parseFromSource(source: scala.io.Source, trafficMapName: String): TrafficGraph = {
    try {
      val lines = source.getLines().toIndexedSeq
      parseLines(lines, trafficMapName)
    } finally {
      source.close()
    }
  }

  private def parseLines(lines: IndexedSeq[String], trafficMapName: String): TrafficGraph = {
    val firstLine = lines(0).split("\\s+")
    val numIntersections = firstLine(0).toInt
    val numStreets = firstLine(1).toInt
    val numVehicles = firstLine(2).toInt

    val intersections = parseIntersections(numIntersections, lines)

    val start = 1 + numIntersections
    val streets = parseStreets(start, numStreets, lines)

    TrafficGraph(numVehicles, intersections, streets)
  }

  private def parseIntersections(numIntersections: Int, lines: IndexedSeq[String]): IndexedSeq[Intersection] =
    (0 until numIntersections).map { i =>
      val line = lines(i + 1)
      val parts = line.split("\\s+")
      val location = FloatLocation(parts(0).toFloat, parts(1).toFloat)
      val signalType = TrafficSignalType.valueOf(parts(2))
      val numPorts = (parts.length - 3) / 2
      val ports: IndexedSeq[Port] =
        (0 until numPorts).map { j =>
          val idx = 3 + j * 2
          Port(j, parts(idx).toDouble, parts(idx + 1).toInt)
        }
      Intersection(i, location, ports, signalType)
    }

  /**
   * More than one street is not allowed to connect to the same port on a node. Each street is bidirectional.
   * Format
   * intersectionId_i1 port_i intersectionId_j1 port_j
   */
  private def parseStreets(start: Int, numStreets: Int, lines: IndexedSeq[String]): IndexedSeq[Street] = {
    var portSet: Set[(Int, Int)] = Set.empty
    (0 until numStreets).map { i =>
      val line = lines(i + start)
      val parts = line.split("\\s+")

      val intersectionIdx1 = parts(0).toInt
      val port1 = parts(1).toInt
      val intersectionIdx2 = parts(2).toInt
      val port2 = parts(3).toInt

      portSet = addNodePortIfAvailable(intersectionIdx1, port1, portSet)
      portSet = addNodePortIfAvailable(intersectionIdx2, port2, portSet)

      Street(intersectionIdx1, port1, intersectionIdx2, port2)
    }
  }

  private def addNodePortIfAvailable(intersectionIdx: Int, portIdx: Int, nodePortSet: Set[(Int, Int)]): Set[(Int, Int)] = {
    val p = (intersectionIdx, portIdx)
    if (nodePortSet.contains(p)) {
      throw new IllegalStateException(s"Each intersection port can only have one street attached. More than one at ${p._1} to ${p._2}.")
    } else nodePortSet + p
  }
}
