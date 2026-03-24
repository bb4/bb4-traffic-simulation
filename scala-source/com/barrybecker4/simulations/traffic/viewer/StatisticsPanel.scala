package com.barrybecker4.simulations.traffic.viewer

import com.barrybecker4.common.format.FormatUtil
import com.barrybecker4.simulations.traffic.simulation.SimulationState
import com.barrybecker4.simulations.traffic.vehicles.{VehicleSprite, VehicleSpriteManager, VehicleStatistics}
import org.graphstream.graph.Graph

import java.awt.BorderLayout
import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}
import javax.swing.{JPanel, JTable}
import javax.swing.table.{AbstractTableModel, TableColumn}

class StatisticsPanel(graph: Graph, state: SimulationState, spriteManager: VehicleSpriteManager) extends JPanel {

  private val data = Array(
    Array("Time", "<total time>", "last 2 seconds"),
    Array("Distance", "<total dist>", "<inc distance>"),
    Array("Avg speed per vehicle", "<avg speed>", "<current avg speed>")
  )

  class StatsTableModel(data: Array[Array[String]]) extends AbstractTableModel {
    private val columnNames = Array("Metric", "Total", "Current")
    override def getRowCount: Int = data.length
    override def getColumnCount: Int = columnNames.length
    override def getValueAt(rowIndex: Int, columnIndex: Int): Any = data(rowIndex)(columnIndex)
    override def getColumnName(column: Int): String = columnNames(column)
  }

  private val table = createMetricTable()
  this.setLayout(new BorderLayout())
  this.add(table, BorderLayout.CENTER)

  private val startTime = System.currentTimeMillis()

  val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)

  scheduler.scheduleAtFixedRate(() => updateStats(), 0, 2, TimeUnit.SECONDS)

  private def createMetricTable(): JTable = {
    val tableModel = new StatsTableModel(data)
    val table = new JTable(tableModel)
    initColumn(table.getColumn("Metric"), 100)
    initColumn(table.getColumn("Total"), 200)
    initColumn(table.getColumn("Current"), 200)
    table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS)
    table
  }

  private def initColumn(col: TableColumn, width: Int): Unit = {
    col.setMinWidth(100)
    col.setMaxWidth(500)
    col.setPreferredWidth(width)
    col.setResizable(true)
  }

  private def updateStats(): Unit = {
    val allVehicles = getAllVehicles
    val stats = VehicleStatistics(allVehicles)
    val numVehicles = allVehicles.size
    println("numVehicles = " + numVehicles)

    val elapsed: Double = (System.currentTimeMillis() - startTime) / 1000.0
    val elapsedSafe = elapsed.max(1e-6)
    data(0)(1) = FormatUtil.formatNumber(elapsed) + " seconds"
    data(1)(1) = FormatUtil.formatNumber(stats.getTotalDistance) + " meters"
    data(1)(2) = FormatUtil.formatNumber(stats.getIncrementalDistance) + " meters"
    if (numVehicles == 0) {
      data(2)(1) = "—"
      data(2)(2) = "—"
    } else {
      data(2)(1) = FormatUtil.formatNumber((stats.getTotalDistance / numVehicles) / elapsedSafe) + " m/s"
      data(2)(2) = FormatUtil.formatNumber((stats.getIncrementalDistance / numVehicles) / 2.0) + " m/s"
    }
    stats.resetIncrementalDistance()
    this.repaint()
  }

  private def getAllVehicles: Set[VehicleSprite] =
    state.allSimVehicles.flatMap { v =>
      Option(spriteManager.getSprite(v.id)).map(_.asInstanceOf[VehicleSprite])
    }.toSet

}

