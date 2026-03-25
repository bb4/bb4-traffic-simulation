package com.barrybecker4.simulations.traffic.viewer

import TrafficViewerFrame.{PARSER, SUFFIX, TRAFFIC_GRAPHS_PREFIX}
import com.barrybecker4.simulations.traffic.demo.TrafficSimulationBootstrap
import com.barrybecker4.simulations.traffic.roadnet.{TrafficGraph, TrafficGraphParser}
import com.barrybecker4.simulations.traffic.simulation.TrafficSimulationConfig
import com.barrybecker4.simulations.traffic.viewer.adapter.{TrafficGraphBundle, TrafficStreamAdapter}
import org.graphstream.graph.implementations.MultiGraph
import org.graphstream.ui.layout.springbox.implementations.SpringBox
import org.graphstream.ui.view.Viewer

import java.awt.BorderLayout
import java.io.File
import javax.swing.*
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

/** Draws the traffic graph and simulates cars on streets.
 */
object TrafficViewerFrame {
  private val TRAFFIC_GRAPHS_PREFIX = "scala-test/com/barrybecker4/simulations/traffic/data/"
  private val PARSER: TrafficGraphParser = TrafficGraphParser()
  private val SUFFIX: String = ".txt"
}

class TrafficViewerFrame extends JFrame {
  private var graphViewer: Viewer = null
  private var embeddedView: JPanel = null

  createMenu()
  setupInitialAppearance()

  /** Without this, the frame never shows and the JVM often exits as soon as `main` returns. */
  private def setupInitialAppearance(): Unit = {
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
    setTitle("Traffic Simulation")
    setLayout(new BorderLayout())
    val hint = new JLabel("File → Open traffic map to load a map.", SwingConstants.CENTER)
    hint.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24))
    add(hint, BorderLayout.CENTER)
    setSize(900, 700)
    setLocationRelativeTo(null)
    setVisible(true)
  }

  private def createMenu(): Unit = {
    val myMenuBar: JMenuBar = new JMenuBar()
    val fileMenu = new JMenu("File")
    val openTrafficGraphItem = createOpenTrafficGraphItemOption()
    fileMenu.add(openTrafficGraphItem)
    myMenuBar.add(fileMenu)
    setJMenuBar(myMenuBar)
  }

  private def createOpenTrafficGraphItemOption(): JMenuItem = {
    val openItem = new JMenuItem("Open traffic map")
    openItem.addActionListener(_ => loadTrafficGraph())
    openItem
  }

  private def loadTrafficGraph(): Unit = {
    val fileChooser = new JFileChooser()
    fileChooser.setCurrentDirectory(new File(TRAFFIC_GRAPHS_PREFIX))

    val returnValue = fileChooser.showOpenDialog(TrafficViewerFrame.this)
    if (returnValue == JFileChooser.APPROVE_OPTION) {
      val selectedFile = fileChooser.getSelectedFile
      println("selected file is " + selectedFile.getName)

      val trafficGraph = loadTrafficGraph(selectedFile)
      val config = TrafficSimulationConfig.Default
      val adapter = TrafficStreamAdapter(trafficGraph, config)
      val bundle = adapter.build()
      val initialSpeed = 1.0
      setGraph(bundle, trafficGraph.numVehicles, initialSpeed, config, "Traffic Demo")
    }
  }

  private def loadTrafficGraph(file: File): TrafficGraph = {
    val graphName = getGraphName(file.getName)
    loadTrafficGraphFromName(graphName)
  }

  def setGraph(
      bundle: TrafficGraphBundle,
      numVehicles: Int,
      initialSpeed: Double,
      config: TrafficSimulationConfig,
      title: String
  ): Unit = {
    val graph = bundle.graph

    if (graphViewer != null) {
      graphViewer.close()
      graphViewer = null
    }
    if (embeddedView != null) {
      remove(embeddedView)
      embeddedView = null
    }
    getContentPane.removeAll()

    graphViewer = graph.display(false)
    if (graph.getNodeCount > 0 && graph.getNode(0).hasAttribute("xyz")) graphViewer.disableAutoLayout()
    else graphViewer.enableAutoLayout(new SpringBox())

    embeddedView = graphViewer.getDefaultView.asInstanceOf[JPanel]

    this.setTitle(title)

    val state = TrafficSimulationBootstrap.createState(config, bundle)
    val spriteGen = TrafficSimulationBootstrap.addSprites(bundle, state, numVehicles, initialSpeed, config)

    this.setLayout(new BorderLayout())
    val statsPanel = new StatisticsPanel(graph, state, spriteGen.getSpriteManager)
    this.add(statsPanel, BorderLayout.NORTH)
    this.add(embeddedView, BorderLayout.CENTER)

    this.repaint()
    setVisible(true)

    val displayFuture: Future[Unit] = Future {
      TrafficSimulationBootstrap.runOrchestrator(bundle, config, spriteGen, graphViewer.newViewerPipe())
    }
    displayFuture.onComplete {
      case scala.util.Success(_) =>
        println("TrafficDemo completed successfully.")
      case scala.util.Failure(exception) =>
        println(s"TrafficDemo run failed with exception: $exception")
        val cause = exception.getCause
        cause.printStackTrace()
    }
  }

  private def getGraphName(fileName: String): String = {
    fileName.substring(0, fileName.length - SUFFIX.length)
  }

  private def loadTrafficGraphFromFile(file: File): TrafficGraph = {
    val name = file.getName
    val source: Source = Source.fromFile(file.getAbsolutePath)
    PARSER.parseFromSource(source, name)
  }

  private def loadTrafficGraphFromName(name: String): TrafficGraph = {
    loadTrafficGraphFromFile(new File(TRAFFIC_GRAPHS_PREFIX + name + SUFFIX))
  }
}
