package ProjectMain;

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
import javafx.scene.layout.Pane; 

// Import the correct TraaS library (DLR version)
import de.tudresden.sumo.cmd.*;
import de.tudresden.ws.container.SumoPosition2D;
import it.polito.appeal.traci.SumoTraciConnection;

// Java XML Parsing Imports
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

// Java Imports
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Controller {

    // --- FXML View Elements ---
    
    @FXML private StackPane mapControlsContainer;
    @FXML private VBox mapControlsVBox;
    @FXML private ScrollPane leftControlPanel;
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

    // Sumo-GUI Integration (Placeholder fields to match FXML if present)
    @FXML private TextField pathToSumocfgFile;
    @FXML private TextField pathToSumoGui;
    @FXML private Button insertSumocfgFile;
    @FXML private Button startSumoGuiButton;

    // Map & Log
    @FXML private Pane mapPane; 
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
        log("Initialization started.");
        try {
            // Find the .net.xml URL relative to resources
            URL netUrl = getClass().getResource("/frauasmap.net.xml");
            if (netUrl == null) {
                log("FATAL ERROR: frauasmap.net.xml URL not found in classpath. Check /src/main/resources/ folder.");
                return;
            }
            log("SUCCESS: Found net.xml URL: " + netUrl.toExternalForm());
            
            // Find the .sumocfg URL (optional for drawing, needed for sim)
            URL configUrl = getClass().getResource("/frauasmap.sumocfg");
            if (configUrl != null) {
                log("SUCCESS: Found sumocfg URL.");
                try {
                    sumoConfigPath = new File(configUrl.toURI()).getAbsolutePath();
                } catch (Exception e) {
                    log("Warning: Could not convert config URL to file path.");
                }
            }
            
            // --- DRAWING TRIGGER ---
            // We pass the URL directly to ensure robust loading
            drawMapNetwork(netUrl); 

        } catch (Exception e) {
            log("FATAL ERROR initializing file paths or drawing map: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Parses the .net.xml file via URL, determines map boundaries, and draws the road network.
     */
    private void drawMapNetwork(URL netXmlUrl) {
        log("--- Starting Map Drawing Process ---");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        
        try {
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            
            // Parse directly from URL
            Document doc = dBuilder.parse(netXmlUrl.toExternalForm()); 
            doc.getDocumentElement().normalize();
            log("SUCCESS: XML document parsed successfully.");

            // --- Step 1B: Find Map Boundary ---
            NodeList locationNodes = doc.getElementsByTagName("location");
            if (locationNodes.getLength() > 0) {
                Element location = (Element) locationNodes.item(0);
                
                // 🔥 FIX: Using "convBoundary" because "netBoundary" was empty/missing in your XML
                String boundaryStr = location.getAttribute("convBoundary");
                String[] boundary = boundaryStr.split(",");
                
                if (boundary.length == 4) {
                    try {
                        minX = Double.parseDouble(boundary[0]);
                        minY = Double.parseDouble(boundary[1]);
                        maxX = Double.parseDouble(boundary[2]);
                        maxY = Double.parseDouble(boundary[3]);
                        log(String.format("SUCCESS: Map bounds set: (%.2f, %.2f) to (%.2f, %.2f)", minX, minY, maxX, maxY));
                    } catch (NumberFormatException e) {
                        log("ERROR: Could not parse boundary numbers: " + boundaryStr);
                    }
                } else {
                    log("ERROR: Invalid boundary format in XML: " + boundaryStr);
                }
            } else {
                log("WARNING: Could not find <location> tag. Drawing may be off-center.");
            }

            NodeList laneNodes = doc.getElementsByTagName("lane");
            int laneCount = laneNodes.getLength();
            
            if (laneCount == 0) {
                 log("WARNING: Found 0 <lane> elements. Nothing to draw.");
                 return;
            }
            
            log(String.format("Found %d lanes. Starting Polyline creation...", laneCount));
            
            // --- Step 2: Iterate and Draw Lanes ---
            for (int i = 0; i < laneNodes.getLength(); i++) {
                Element lane = (Element) laneNodes.item(i);
                String shapeStr = lane.getAttribute("shape");
                
                if (shapeStr.isEmpty()) continue;
                
                Polyline roadShape = new Polyline(); 
                String[] points = shapeStr.split(" ");
                
                for (String point : points) {
                    String[] coords = point.split(",");
                    
                    // FIX: Parse the X and Y coordinates
                    if (coords.length == 2) {
                        try {
                            double x = Double.parseDouble(coords[0]);
                            double y = Double.parseDouble(coords[1]);

                            // Transform and add to shape
                            roadShape.getPoints().add(transformX(x)); 
                            roadShape.getPoints().add(transformY(y));
                        } catch (NumberFormatException e) {
                            // Skip bad points
                        }
                    }
                }
                
                roadShape.setStroke(Color.GRAY);
                roadShape.setStrokeWidth(5);
                mapPane.getChildren().add(roadShape); 
            }
            
            log("SUCCESS: Map drawing complete! Added " + laneCount + " lanes.");

        } catch (Exception e) {
            log("FATAL ERROR during XML parsing/drawing: " + e.getMessage());
            e.printStackTrace();
        }
        log("--- Map Drawing Process Ended ---");
    }

    /**
     * This method is called when the "Start Simulation" button is clicked.
     */
    @FXML
    private void startSimulation() {
        log("Starting simulation... (SUMO LOGIC IS COMMENTED OUT FOR UI TEST)");
        // Logic to start TraaS would go here
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
                 // ... simulation loop ...
            }
        };
        simulationTimer.start();
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
        // Logic to destroy SUMO process would go here
    }

    // --- Coordinate Transformation Helpers ---

    private double transformX(double sumoX) {
        // (sumoX - minX) shifts map to 0
        // + 50.0 adds padding
        return (sumoX - minX) + 50.0;
    }
    
    private double transformY(double sumoY) {
        // (maxY - sumoY) flips the Y axis (SUMO is up, JavaFX is down)
        // + 50.0 adds padding
        return (maxY - sumoY) + 50.0;
    }

    // --- Utility Helper ---
    
    private void log(String message) {
        System.out.println(message); 
        if (logLabel != null) {
            String oldLog = logLabel.getText();
            logLabel.setText(message + "\n" + oldLog);
        }
    }
    	
    // --- Placeholder Action Methods to satisfy FXML ---
    @FXML private void pauseSimulation() {}
    @FXML private void stepSimulation() {}
    @FXML private void injectVehicle() {}
    @FXML private void startSumoGUI() {}
    @FXML private void insertSumoConfigFile() {}
}