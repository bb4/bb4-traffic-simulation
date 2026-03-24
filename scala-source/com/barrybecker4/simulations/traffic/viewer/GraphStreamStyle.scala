package com.barrybecker4.simulations.traffic.viewer

import scala.io.Source
import scala.util.Using

/** Loads the GraphStream CSS bundled with the viewer (single path used by stream adapter and demos). */
object GraphStreamStyle {
  private val StyleSheetPath =
    "scala-source/com/barrybecker4/simulations/traffic/viewer/adapter/traffic.css"

  def loadStyleSheet(): String =
    Using(Source.fromFile(StyleSheetPath))(_.mkString)
      .getOrElse(throw new RuntimeException(s"Failed to read the style sheet from $StyleSheetPath"))
}
