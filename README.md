# bb4-traffic-simulation

Simulates traffic flow using graphs and sprites (GraphStream + a domain model in `roadnet` / `simulation`).

## How to run

Use Java 21+ (`java --version`). With [sdkman](https://sdkman.io/) you can install and switch JDK versions.

```bash
./gradlew run
```

Or run `com.barrybecker4.simulations.traffic.TrafficApp` from IntelliJ.

The window opens with **File → Open traffic map**. Example data files live under `scala-test/com/barrybecker4/simulations/traffic/data/` (e.g. `dumbTrafficMap1.txt`).

A second entry point, `TrafficDemoApplication`, loads a default map and runs the same simulation pipeline (no menu).

## Tuning

Simulation timing and physics are centralized in [`TrafficSimulationConfig`](scala-source/com/barrybecker4/simulations/traffic/simulation/TrafficSimulationConfig.scala):

- **`deltaTimeSecs`** — simulation timestep per tick (default `0.02`).
- **`realtimeSpeedFactor`** — scales wall-clock sleep: `sleepMs = (deltaTimeSecs * 1000 * realtimeSpeedFactor).toLong`. Default **`0.1`** matches the older demo (`Thread.sleep(2)` ms per tick with `delta=0.02`). Use **`0`** for no sleep (run as fast as the CPU allows).

## Tests

```bash
./gradlew test
```

![traffic-app](images/traffic-app.png)
