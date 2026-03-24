package com.barrybecker4.simulations.traffic.signals

import com.barrybecker4.simulations.traffic.simulation.TrafficSimulationConfig

enum TrafficSignalType(val create: (Int, TrafficSimulationConfig) => TrafficSignal) {
  case DUMB_TRAFFIC_SIGNAL extends TrafficSignalType((n, c) => new DumbTrafficSignal(n, c))
  case SEMAPHORE_TRAFFIC_SIGNAL extends TrafficSignalType((n, c) => new SemaphoreTrafficSignal(n, c))
}