Here is the updated `README.md` with the file tree integrated into a new **Project Structure** section. I have adjusted the section numbering accordingly to keep the document logical.

---

# Real-Time Traffic Simulation (OOP Java Project)
*Winter 2025-2026 | Prof. Dr.-Eng. Ghadi Mahmoudi*

This repository contains the source code for the "Real-Time Traffic Simulation" group project for the Object-Oriented Programming in Java module.

---

## 1. Project Overview

This project is a Java-based application that connects to the SUMO (Simulation of Urban MObility) traffic simulator in real-time. It provides a platform to visualize, control, and analyze urban traffic flow. The system features an interactive GUI built with JavaFX, an object-oriented wrapper for the TraaS API, and tools for real-time data analysis and reporting.

### Core Features

- **Live SUMO Integration:** Connects to a running SUMO instance via the TraaS API.
- **Interactive Map Visualization:** Renders the road network, moving vehicles, and traffic light states.
- **Simulation Control:** Allows users to inject vehicles, control vehicle parameters, and manually manage traffic light phases.
- **Statistics & Analytics:** Tracks and displays real-time metrics like average speed, vehicle density, and travel time.
- **Exportable Reports:** Generates CSV and PDF summaries of simulation data.

## 2. Team Members

| Name          | Role                  | GitHub      |
|---------------|-----------------------|-------------|
| `[Your Name]` | Team Lead / Backend   | `[@username]` |
| `[Your Name]` | Frontend (JavaFX)     | `[@username]` |
| `[Your Name]` | SUMO & Data           | `[@username]` |
| `[Your Name]` | Docs & QA             | `[@username]` |
| `[Your Name]` | (Role)                | `[@username]` |

## 3. Technology Stack

- **IDE**: Eclipse IDE
- **Build Tool**: Apache Maven
- **Language**: Java (JDK 17)
- **GUI**: JavaFX (with FXML and Scene Builder)
- **Simulation**: SUMO
- **API**: TraaS (`TraaS.jar`)
- **Version Control**: Git & GitHub

## 4. Project Structure

The project follows a standard Maven directory structure, separating logic (Model), interface (View), and event handling (Controller).

```text
traffic-simulator
├── README.md                  # Project overview and setup instructions
├── lib
│   └── TraaS.jar              # External Library: Traffic Control Interface (TraCI) as a Service
├── pom.xml                    # Maven: Manages dependencies (JavaFX, JUnit, etc.)
├── src
│   ├── main
│   │   ├── java
│   │   │   ├── controller
│   │   │   │   ├── MainController.java        # [UI LOGIC] Handles buttons, timers, and connects View to Model
│   │   │   │   └── MapInteractionHandler.java # [UX] Handles Mouse Drag/Scroll on the map
│   │   │   ├── model
│   │   │   │   ├── ReportManager.java         # [IO] Exports stats to CSV/PDF
│   │   │   │   ├── SimulationManager.java     # [CORE] Runs the thread, talks to SUMO
│   │   │   │   ├── StatisticsManager.java     # [DATA] Calculates averages and counters
│   │   │   │   ├── infrastructure
│   │   │   │   │   ├── SumoMap.java           # [DATA] Holds static road network data
│   │   │   │   │   ├── SumoTrafficlight.java  # [DATA] Holds traffic light state
│   │   │   │   │   └── mapInSumo.txt          # Helper/Debug text for map data
│   │   │   │   └── vehicles
│   │   │   │       └── Vehicle.java           # [ENTITY] Base class for cars/buses
│   │   │   ├── testjava
│   │   │   │   └── TestSumo.java              # [TEST] Quick console test to verify TraCI connection
│   │   │   ├── util
│   │   │   │   └── CoordinateConverter.java   # [MATH] Converts meters to pixels
│   │   │   └── view
│   │   │       ├── MainGUI.java               # [ENTRY] public static main, loads FXML
│   │   │       ├── Renderer.java              # [DRAWING] Draws shapes on the JavaFX Pane
│   │   │       └── javafxApi.txt              # API notes (documentation)
│   │   └── resources
│   │       ├── frauasmap.net.xml              # SUMO Network (Roads/Junctions)
│   │       ├── frauasmap.osm                  # OpenStreetMap source file
│   │       ├── frauasmap.rou.xml              # SUMO Routes (Vehicle paths)
│   │       ├── frauasmap.sumocfg              # SUMO Configuration (binds net+rou)
│   │       ├── gui
│   │       │   ├── MainView.fxml              # [LAYOUT] XML definition of the User Interface
│   │       │   └── testDrawMap.fxml           # Testing layout
│   │       ├── minimal.sumocfg                # Simplified config for testing
│   │       └── trips.trips.xml                # Generated trips data
│   └── test
│       ├── java                               # Location for JUnit tests
│       └── resources                          # Test-specific resources
```

## 5. How to Run

### Prerequisites

- **Java 17+**: Must be installed and added to your `PATH`.
- **Maven**: Must be installed and added to your `PATH`.
- **SUMO**: Must be installed, and the `sumo` executable must be in your `PATH`.
- **TraaS.jar**: You must place the `TraaS.jar` file inside a `lib/` folder in the project's root directory.

### Running the Simulation

1.  **Clone the Repository:**
    ```bash
    git clone https://github.com/KiwisCodes/traffic-simulator.git
    cd traffic-simulator
    ```

2.  **Start the SUMO Server:**
    Open a terminal and run SUMO with a configuration file and the `--remote-port` argument. (You must provide your own `.sumocfg` file or use the one provided in `src/main/resources`).
    ```bash
    # Example using the provided config file
    sumo -c src/main/resources/frauasmap.sumocfg --remote-port 9999
    ```

3.  **Run the Java Application:**
    Open a *second terminal* in the project root and use Maven to run the application.
    ```bash
    # This will compile and run the JavaFX application
    mvn clean javafx:run
    ```

### Building a Runnable .jar

You can create a single, all-inclusive `.jar` file (which includes JavaFX and TraaS) by running:

```bash
mvn clean package
```

This will create a file like `traffic-simulator-1.0-SNAPSHOT.jar` in the `target/` directory. You can run it with:

```bash
java -jar target/traffic-simulator-1.0-SNAPSHOT.jar
```

## 6. Milestone Status

- [x] **Milestone 1** (System Design & Prototype): Due 27.11.2025
- [ ] **Milestone 2** (Functional Prototype): Due 14.12.2025
- [ ] **Final Submission**: Due 18.01.2026
