package com.barrybecker4.simulations.traffic.roadnet

import com.barrybecker4.common.geometry.FloatLocation
import com.barrybecker4.simulations.traffic.roadnet.model.{Intersection, Street}

import scala.collection.mutable

/**
 * Streets are undirected; `neighborsOf` returns intersections reachable by one street segment.
 */
case class TrafficGraph(numVehicles: Int,
                        intersections: IndexedSeq[Intersection], 
                        streets: IndexedSeq[Street]) {
  
  private val neighborMap: mutable.Map[Int, mutable.Set[Int]] = mutable.Map.empty

  computeNeighborsMap()

  def numIntersections: Int = intersections.size
  def getLocation(intersectionIdx: Int): FloatLocation = intersections(intersectionIdx).location
  def getIntersection(intersectionIdx: Int): Intersection = intersections(intersectionIdx)
  def neighborsOf(v: Int): Set[Intersection] =
    neighborMap.getOrElse(v, mutable.Set.empty).map(i => intersections(i)).toSet
  
  private def computeNeighborsMap(): Unit = {
    for (street <- streets) {
      neighborMap.getOrElseUpdate(street.intersectionIdx1, mutable.Set()) += street.intersectionIdx2
      neighborMap.getOrElseUpdate(street.intersectionIdx2, mutable.Set()) += street.intersectionIdx1
    }
  }

}
