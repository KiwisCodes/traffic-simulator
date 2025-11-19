package gui;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.scene.layout.Pane; // <-- CORRECT IMPORT FOR PANE

// Import the correct TraaS library (DLR version)
import de.tudresden.sumo.cmd.Vehicle;
import de.tudresden.ws.container.SumoPosition2D;
import it.polito.appeal.traci.SumoTraciConnection;

// Java XML Parsing Imports
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

// Java Imports
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Controller {

    // --- FXML View Elements ---
    
    // --- UI elements from "Simplified Layout" FXML ---
    @FXML private StackPane mapControlsContainer;
    @FXML private VBox mapControlsVBox;
    @FXML private ScrollPane leftControlPanel;
    // @FXML private Button menuToggleButton; // <--REMOVED - No longer needed
    @FXML private ScrollPane mapScrollPane;
    
    // Simulation Control
    @FXML private Button startButton;
    @FXML private Button pauseButton;
    @FXML private Button stepButton;
    
    // Vehicle Actions
    @FXML private TextField vehicleIdField;
    @FXML private TextField routeIdField;
    @FXML private Button injectVehicleButton;
    @FXML private Button setVehicleSpeedButton;
    @FXML private TextField vehicleSpeedField;
    @FXML private Button setVehicleColorButton;
    @FXML private TextField vehicleColorField;
    
    // Traffic Light Actions
    @FXML private TextField trafficLightIdField;
    @FXML private Button setRedPhaseButton;
    @FXML private Button setGreenPhaseButton;
    @FXML private Button resumeAutoButton;
    @FXML private Button setPhaseDurationButton;
    @FXML private TextField phaseDurationField;
    @FXML private CheckBox adaptiveTrafficCheck;

    // Vehicle Filtering
    @FXML private TextField filterColorField;
    @FXML private TextField filterSpeedField;
    @FXML private TextField filterEdgeField;
    @FXML private Button applyFilterButton;
    @FXML private Button clearFilterButton;

    // Stress Testing
    @FXML private TextField stressEdgeField;
    @FXML private TextField stressCountField;
    @FXML private Button stressTestButton;

    // Live Statistics
    @FXML private Label simStepLabel;
    @FXML private Label vehicleCountLabel;
    @FXML private Label avgSpeedLabel;
    @FXML private Label avgTravelTimeLabel;
    @FXML private Label congestionLabel;
    @FXML private Button showChartsButton;

    // Data Export
    @FXML private CheckBox exportFilterCheck;
    @FXML private Button exportCsvButton;
    @FXML private Button exportPdfButton;

    // Map & Log
    @FXML private Pane mapPane; // <-- This is now correct due to the import
    @FXML private Label logLabel;
    @FXML private Button zoomInButton;
    @FXML private Button zoomOutButton;
    @FXML private Button resetViewButton;
    @FXML private ToggleButton toggle3DButton;
    
    // --- SUMO & TraaS ---
    private Process sumoProcess;
    private SumoTraciConnection sumoConnection;
    private String sumoConfigPath;
    private String netFilePath;

    // --- Visualization ---
    private Map<String, Circle> vehicleShapes = new HashMap<>();
    private double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
    private double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;
    private AnimationTimer simulationTimer;

    /**
     * This method is called by JavaFX *after* the FXML file is loaded.
     */
    @FXML
    public void initialize() {
        
        // --- NEW: SIMPLIFIED UI ---
        // The leftControlPanel is now permanent, so no special
        // listener code is needed anymore. It's much cleaner!
        // --- END OF NEW UI LOGIC ---

        // Set up file paths using the files in the 'resources' folder
        try {
            // Find the .sumocfg file
            URL configUrl = getClass().getResource("/frauasmap.sumocfg");
            if (configUrl == null) {
                log("Error: frauasmap.sumocfg not found in resources.");
                return;
            }
            sumoConfigPath = new File(configUrl.toURI()).getAbsolutePath();

            // Find the .net.xml file
            URL netUrl = getClass().getResource("/frauasmap.net.xml");
            if (netUrl == null) {
                log("Error: frauasmap.net.xml not found in resources.");
                return;
            }
            netFilePath = new File(netUrl.toURI()).getAbsolutePath();
            
            // Draw the static map *before* the simulation starts
            drawMapNetwork(netFilePath);

        } catch (Exception e) {
            log("Error initializing file paths: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * This method is called when the "Start Simulation" button is clicked.
     */
    @FXML
    private void startSimulation() {
        log("Starting simulation... (SUMO LOGIC IS COMMENTED OUT FOR UI TEST)");
        startButton.setDisable(true); 
        
        /*
        // --- SUMO LOGIC (Commented out for UI testing) ---
        
        try {
            // 1. Start the SUMO Process
            ProcessBuilder pb = new ProcessBuilder("sumo", "-c", sumoConfigPath, "--remote-port", "9999");
            sumoProcess = pb.start();
            log("SUMO process started.");

            // Give SUMO a moment to start up
            Thread.sleep(2000); 

            // 2. Connect to SUMO via TraaS (DLR library)
            sumoConnection = new SumoTraciConnection(9999);
            log("TraaS connected to SUMO.");
            
            // 3. Start the JavaFX AnimationTimer (the main simulation loop)
            startAnimationTimer();

        } catch (Exception e) {
            log("Error starting simulation: " + e.getMessage());
            e.printStackTrace();
            startButton.setDisable(false);
        }
        */
    }
    
    /**
     * Parses the .net.xml file and draws the road network on the mapPane.
     */
    private void drawMapNetwork(String netXmlFile) {
        log("Parsing network file: " + netXmlFile);
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new File(netXmlFile));
            doc.getDocumentElement().normalize();

            NodeList locationNodes = doc.getElementsByTagName("location");
            if (locationNodes.getLength() > 0) {
                Element location = (Element) locationNodes.item(0);
                String[] boundary = location.getAttribute("netBoundary").split(",");
                minX = Double.parseDouble(boundary[0]);
                minY = Double.parseDouble(boundary[1]);
                maxX = Double.parseDouble(boundary[2]);
                maxY = Double.parseDouble(boundary[3]);
                log(String.format("Map bounds: (%.2f, %.2f) to (%.2f, %.2f)", minX, minY, maxX, maxY));
            }

            NodeList laneNodes = doc.getElementsByTagName("lane");
            for (int i = 0; i < laneNodes.getLength(); i++) {
                Element lane = (Element) laneNodes.item(i);
                
                String shapeStr = lane.getAttribute("shape");
                if (shapeStr.isEmpty()) continue;

                Polyline roadShape = new Polyline();
                
                String[] points = shapeStr.split(" ");
                for (String point : points) {
                    String[] coords = point.split(",");
                    double x = Double.parseDouble(coords[0]);
                    double y = Double.parseDouble(coords[1]);
                    
                    roadShape.getPoints().add(transformX(x));
                    roadShape.getPoints().add(transformY(y));
                }

                roadShape.setStroke(Color.GRAY);
                roadShape.setStrokeWidth(1.5);
                mapPane.getChildren().add(roadShape);
            }
            log("Successfully drew " + laneNodes.getLength() + " lanes.");

        } catch (Exception e) {
            log("Error parsing net file: \"Read timed out\". Fallback to console log.");
            // e.printStackTrace(); // Can be noisy
        }
    }

    /**
     * Creates and starts the main simulation loop.
     */
    private void startAnimationTimer() {
        
        /*
        // --- SUMO LOGIC (Commented out for UI testing) ---
        
        simulationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                try {
                    // 1. Advance SUMO
                    sumoConnection.do_timestep();
                    
                    // 2. Get all vehicles that are currently active
                    @SuppressWarnings("unchecked")
                    List<String> vehicleIds = (List<String>)
                        sumoConnection.do_job_get(Vehicle.getIDList());
                    
                    Set<String> seenVehicleIds = new HashSet<>(vehicleIds);

                    // 3. Update existing vehicles and add new ones
                    for (String id : vehicleIds) {
                        // Get position for *each* vehicle individually
                        SumoPosition2D pos = (SumoPosition2D) 
                            sumoConnection.do_job_get(Vehicle.getPosition(id));

                        // Transform coordinates
                        double x = transformX(pos.x);
                        double y = transformY(pos.y);

                        if (vehicleShapes.containsKey(id)) {
                            vehicleShapes.get(id).setCenterX(x);
                            vehicleShapes.get(id).setCenterY(y);
                        } else {
                            Circle car = new Circle(x, y, 4, Color.BLUE);
                            car.setStroke(Color.BLACK);
                            
                            vehicleShapes.put(id, car); 
                            mapPane.getChildren().add(car);
                        }
                    }

                    // 4. Remove vehicles that have left the simulation
                    List<String> vehiclesToRemove = new ArrayList<>();
                    for (String id : vehicleShapes.keySet()) {
                        if (!seenVehicleIds.contains(id)) {
                            vehiclesToRemove.add(id);
                        }
                    }
                    
                    for (String id : vehiclesToRemove) {
                        Circle car = vehicleShapes.remove(id); 
                        mapPane.getChildren().remove(car);
                    }

                } catch (Exception e) {
                    log("Error in simulation step: " + e.getMessage());
                    e.printStackTrace();
                    simulationTimer.stop(); // Stop the loop on error
                    startButton.setDisable(false);
                }
            }
        };
        simulationTimer.start(); // Start the loop
        */
    }

    /**
     * This method is called by MainGUI when the window is closed.
     */
    public void shutdown() {
        log("Shutting down...");
        if (simulationTimer != null) {
            simulationTimer.stop();
        }
        
        /*
        // --- SUMO LOGIC (Commented out for UI testing) ---
        try {
            if (sumoConnection != null) {
                sumoConnection.close();
                log("TraaS connection closed.");
            }
        } catch (Exception e) {
            log("Error closing TraaS: ".concat(e.getMessage()));
        }

        if (sumoProcess != null) {
            sumoProcess.destroy(); 
            try {
                sumoProcess.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log("SUMO process destroyed.");
        }
        */
    }

    // --- Coordinate Transformation Helpers ---

    private double transformX(double sumoX) {
        // 50.0 is just an arbitrary padding from the edge
        return (sumoX - minX) + 50.0;
    }
    
    private double transformY(double sumoY) {
        // (maxY - sumoY) flips the coordinate
        // + 50.0 is an arbitrary padding from the edge
        return (maxY - sumoY) + 50.0;
    }

    // --- Utility Helper ---
    
    /**
     * Appends a message to the log label in the GUI.
     */
    private void log(String message) {
        // Also print to console for debugging
        System.out.println(message); 
        
        if (logLabel != null) {
            String oldLog = logLabel.getText();
            // Show latest log at the top
            logLabel.setText(message + "\n" + oldLog);
        }
    }
}





/*
	// --- Service Link ---

- simManager: SimulationManager

// --- GUI/Map State ---

- simulationTimer: AnimationTimer

- vehicleShapes: Map<String, Circle>

- mapBounds: BoundingBox

- currentScale: double

// --- File Paths ---

- sumoConfigPath: String

- netFilePath: String

// --- FXML UI Elements: Map & Log ---

[FXML] - leftControlPanel: ScrollPane

[FXML] - mapScrollPane: ScrollPane

[FXML] - mapPane: Pane

[FXML] - logLabel: Label

[FXML] - mapControlsContainer: StackPane

[FXML] - mapControlsVBox: VBox

[FXML] - zoomInButton: Button

[FXML] - zoomOutButton: Button

[FXML] - resetViewButton: Button

[FXML] - toggle3DButton: ToggleButton

// --- FXML UI Elements: Sim Control ---

[FXML] - startButton: Button

[FXML] - pauseButton: Button

[FXML] - stepButton: Button

// --- FXML UI Elements: Vehicle Actions ---

[FXML] - vehicleIdField: TextField

[FXML] - routeIdField: TextField

[FXML] - injectVehicleButton: Button

[FXML] - setVehicleSpeedButton: Button

[FXML] - vehicleSpeedField: TextField

[FXML] - setVehicleColorButton: Button

[FXML] - vehicleColorField: TextField

// --- FXML UI Elements: Traffic Light Actions ---

[FXML] - trafficLightIdField: TextField

[FXML] - setRedPhaseButton: Button

[FXML] - setGreenPhaseButton: Button

[FXML] - resumeAutoButton: Button

[FXML] - setPhaseDurationButton: Button

[FXML] - phaseDurationField: TextField

[FXML] - adaptiveTrafficCheck: CheckBox

// --- FXML UI Elements: Vehicle Filtering ---

[FXML] - filterColorField: TextField

[FXML] - filterSpeedField: TextField

[FXML] - filterEdgeField: TextField

[FXML] - applyFilterButton: Button

[FXML] - clearFilterButton: Button

// --- FXML UI Elements: Stress Testing ---

[FXML] - stressEdgeField: TextField

[FXML] - stressCountField: TextField

[FXML] - stressTestButton: Button

// --- FXML UI Elements: Statistics ---

[FXML] - simStepLabel: Label

[FXML] - vehicleCountLabel: Label

[FXML] - avgSpeedLabel: Label

[FXML] - avgTravelTimeLabel: Label

[FXML] - congestionLabel: Label

[FXML] - showChartsButton: Button

// --- FXML UI Elements: Data Export ---

[FXML] - exportFilterCheck: CheckBox

[FXML] - exportCsvButton: Button

[FXML] - exportPdfButton: Button

*/