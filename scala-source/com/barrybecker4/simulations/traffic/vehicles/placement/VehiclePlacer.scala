package com.barrybecker4.simulations.traffic.vehicles.placement

import com.barrybecker4.simulations.traffic.simulation.{RoadTopology, TrafficSimulationConfig}
import com.barrybecker4.simulations.traffic.vehicles.{VehicleSprite, VehicleSpriteManager}
import com.barrybecker4.simulations.traffic.viewer.TrafficGraphUtil
import org.graphstream.graph.{Edge, Graph}
import org.graphstream.ui.spriteManager.Sprite

import java.util.concurrent.atomic.AtomicInteger
import scala.jdk.CollectionConverters.*
import scala.util.Random

/**
 * Determines vehicle placements along all the edges of a provided graph.
 * Do not place vehicles in intersections initially.
 */
object VehiclePlacer {
  private val VEHICLE_COLORS = Array[String](
    "#77225599;",
    "#77663399;",
    "#33775599;",
    "#33227799;"
  )
  private val RND: Random = new Random(0)
}

class VehiclePlacer(
    private val sprites: VehicleSpriteManager,
    private val graph: Graph,
    private val topology: RoadTopology,
    private val config: TrafficSimulationConfig,
    private val numVehicles: Int
) {

  private val streetEdges: List[Edge] =
    graph.edges.iterator().asScala.filter(TrafficGraphUtil.isStreet).toList

  private var totalAllocation: Int = 0

  def placeVehicleSprites(): Unit = {
    val edgeIdToNumVehicles = determineNumVehiclesOnEdges
    allocateVehicles(edgeIdToNumVehicles)
  }

  private def determineNumVehiclesOnEdges: Map[String, Int] = {
    val totalLen = findTotalLengthOfAllEdges
    var edgeIdToNumVehicles = createInitialEdgeAllocations(numVehicles, totalLen)
    edgeIdToNumVehicles = fineTuneEdgeAllocations(edgeIdToNumVehicles, numVehicles)

    val sumAllocatedVehicles = edgeIdToNumVehicles.values.sum
    assert(numVehicles == sumAllocatedVehicles)
    edgeIdToNumVehicles
  }

  private def createInitialEdgeAllocations(numVehicles: Int, totalLen: Double): Map[String, Int] = {
    var edgeIdToNumVehicles = Map.empty[String, Int]
    totalAllocation = 0
    streetEdges.foreach { edge =>
      val edgeId = edge.getId
      val edgeLen = getEdgeLen(edge)
      val expectedAllocation = (numVehicles * edgeLen / totalLen).toInt
      val maxAllocation = maxVehiclesForEdgeLength(edgeLen)
      if (expectedAllocation > maxAllocation)
        throw new IllegalArgumentException(
          s"Trying to allocate more vehicles ($expectedAllocation) than the street will hold ($maxAllocation)!"
        )
      edge.setAttribute("maxAllocation", Integer.valueOf(maxAllocation))
      val delta = expectedAllocation
      assert(delta >= 0)
      val min = math.max(0, expectedAllocation - delta)
      val max = math.min(maxAllocation, min + 2 * delta)
      val randomAllocation = min + VehiclePlacer.RND.nextInt(max - min + 1)
      assert(randomAllocation <= maxAllocation)
      totalAllocation += randomAllocation
      edgeIdToNumVehicles += (edgeId -> randomAllocation)
    }
    edgeIdToNumVehicles
  }

  private def maxVehiclesForEdgeLength(edgeLen: Double): Int =
    (edgeLen / (config.placerMinGap + config.placerVehicleLength)).toInt

  private def fineTuneEdgeAllocations(edgeIdToNumVehicles: Map[String, Int], numVehicles: Int): Map[String, Int] = {
    var edgeIdToNum = edgeIdToNumVehicles
    val edgeIds = edgeIdToNum.keySet.toArray
    while (totalAllocation > numVehicles) {
      val rndId = edgeIds(VehiclePlacer.RND.nextInt(edgeIds.length))
      if (edgeIdToNum(rndId) > 0) {
        edgeIdToNum += (rndId -> (edgeIdToNum(rndId) - 1))
        totalAllocation -= 1
      }
    }

    while (totalAllocation < numVehicles) {
      val rndId = edgeIds(VehiclePlacer.RND.nextInt(edgeIds.length))
      val edge = graph.getEdge(rndId)
      if (edgeIdToNum(rndId) < getMaxAllocation(edge)) {
        edgeIdToNum += (rndId -> (edgeIdToNum(rndId) + 1))
        totalAllocation += 1
      }
    }
    edgeIdToNum
  }

  private def allocateVehicles(edgeIdToNumVehicles: Map[String, Int]): Unit = {
    val spriteCt = new AtomicInteger
    streetEdges.foreach { edge =>
      val numVehiclesToAdd = edgeIdToNumVehicles(edge.getId)
      placeVehiclesForEdge(edge, numVehiclesToAdd, spriteCt)
    }
    println("done allocating vehicles on edge")
  }

  private def placeVehiclesForEdge(edge: Edge, numVehiclesToAdd: Int, spriteCount: AtomicInteger): Unit = {
    if (numVehiclesToAdd == 0) return
    val maxAllocation = getMaxAllocation(edge)
    val spriteSlots = new Array[Sprite](maxAllocation)
    assert(numVehiclesToAdd <= spriteSlots.length)
    System.out.println(
      s"now adding $numVehiclesToAdd vehicles to edge ${edge.getId} total avail slots = ${spriteSlots.length} with maxAllocation=$maxAllocation"
    )
    for (_ <- 0 until numVehiclesToAdd) {
      var positionIdx = VehiclePlacer.RND.nextInt(spriteSlots.length)
      while (spriteSlots(positionIdx) != null)
        positionIdx = (positionIdx + 1) % spriteSlots.length
      val sprite = sprites.addSprite(s"${spriteCount.get()}")
      val color = VehiclePlacer.VEHICLE_COLORS(VehiclePlacer.RND.nextInt(VehiclePlacer.VEHICLE_COLORS.length))
      sprite.setAttribute("ui.style", s"fill-color: $color")
      spriteCount.incrementAndGet()
      val pos = 0.01 + 0.98 * positionIdx.toDouble / spriteSlots.length
      sprite.setPosition(pos)
      spriteSlots(positionIdx) = sprite
      sprite.asInstanceOf[VehicleSprite].attachToEdge(edge.getId)
    }
  }

  private def findTotalLengthOfAllEdges: Double =
    streetEdges.map(e => getEdgeLen(e)).sum

  private def getMaxAllocation(edge: Edge): Int =
    edge.getAttribute("maxAllocation", classOf[Object]).asInstanceOf[Integer].intValue()

  private def getEdgeLen(edge: Edge): Double =
    topology.length(edge.getId)
}
