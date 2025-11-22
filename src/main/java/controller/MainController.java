package controller;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.scene.layout.Pane;

// Model & View Imports
import model.SimulationManager;
import model.infrastructure.SumoMap;
import model.vehicles.SumoVehicle;
import view.Renderer;
import util.CoordinateConverter; // Ensure this is imported from your util/view package

// Java Imports
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainController {

    // --- FXML View Elements ---
    @FXML private ScrollPane leftControlPanel;
    @FXML private ScrollPane mapScrollPane;
    @FXML private StackPane rootStackPane;

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

    // Filtering
    @FXML private TextField filterColorField;
    @FXML private TextField filterMinSpeedField;
    @FXML private TextField filterEdgeField;
    @FXML private Button applyFilterButton;
    @FXML private Button clearFilterButton;

    // Stress Testing
    @FXML private TextField stressEdgeField;
    @FXML private TextField stressCountField;
    @FXML private Button stressTestButton;

    // Sumo-GUI Integration
    @FXML private TextField pathToSumocfgFile;
    @FXML private TextField pathToSumoGui; 
    @FXML private Button insertSumocfgButton;
    @FXML private Button startSumoGuiButton;

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
    @FXML private Pane baseMapPane;
    @FXML private Pane lanePane;     // Static roads go here
    @FXML private Pane junctionPane;
    @FXML private Pane routePane;
    @FXML private Pane carPane;      // Dynamic cars go here
    @FXML private Pane busPane;
    @FXML private Pane truckPane;
    @FXML private Pane bikePane;
    @FXML private Label logLabel;
    @FXML private Button zoomInButton;
    @FXML private Button zoomOutButton;
    @FXML private Button resetViewButton;
    @FXML private ToggleButton toggle3DButton;

    // --- Logic & State ---
    private SimulationManager simManager;
    private Renderer renderer; 
    private CoordinateConverter converter;
    
    // --- THREAD MANAGEMENT ---
    // 1. UI Thread: Handled by JavaFX & AnimationTimer
    private AnimationTimer uiLoop; 
    
    // 2. Background Threads: Handled by ExecutorService
    // Pool size 2: One for Simulation Engine, One for Statistics
    private ExecutorService threadPool; 
    private final int NUMBER_OF_THREADS = 2; 
    
    // Flags
    private volatile boolean isSimulationRunning = false;

    // --- Visualization ---
    // Map to track visual shapes: ID -> Shape (Used to update positions)
    private Map<String, Shape> vehicleVisuals = new HashMap<>();
    private Group mapContentGroup; // Container for zooming/panning

    // Scaling constants
    private final double PADDING = 50.0;
    
    // --- Initialization ---

    public MainController() {
        // 1. Initialize Model
        this.simManager = new SimulationManager(); // Logic Engine
        
        // 2. Initialize View Helpers
        this.converter = new CoordinateConverter(); // Math Helper
        this.renderer = new Renderer(); // Visual Factory
        
        // 3. Initialize Thread Pool
        this.threadPool = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    }
    
    // Main entry point if running standalone (optional)
    public static void main(String[] args) {
        // JavaFX launching logic usually goes in MainGUI.java
    }

    @FXML
    public void initialize() {
        log("Controller initialized. Waiting to start...");
        
//        // Prepare the Map Group for zooming/panning
//        mapContentGroup = new Group();
//        // Add panes in order (Bottom to Top)
//        mapContentGroup.getChildren().addAll(baseMapPane, lanePane, junctionPane, routePane, carPane, busPane, truckPane, bikePane);
//        
//        rootStackPane.getChildren().clear();
//        rootStackPane.getChildren().add(mapContentGroup);
    }

    // --- ACTION METHODS ---

    @FXML 
    private void startSimulation() {
        // 1. Connect to SUMO (Blocking Call - runs on UI thread currently, 
        //    but acceptable for startup. Ideally, use Task<> for this too).
        log("Attempting to connect to SUMO...");
        boolean connected = this.simManager.startConnection();

        if (connected) {
            log("Connected! Preparing simulation...");
            isSimulationRunning = true;

            // --- A. SETUP MAP ---
            // Now that we are connected, we have map bounds. Setup converter.
            SumoMap sumoMap = this.simManager.getSumoMap();
            this.renderer.setConverter(sumoMap);
            //insde the renderer there is a converter, inside this converter there is the sumoMap info
            /*
             * The Lambda () -> { ... }: This is the code you want to run in the background. It's an anonymous function that defines the task.
             */
            //Draw just the lanes first
	         // DRAW LANES (The code you need)
	         // We ask the renderer to create the Group of lanes using the active connection
	         Group lanesGroup = this.renderer.createLaneGroup(this.simManager.getConnection(), sumoMap);
	
	         // 4. Add the lanes to the GUI Pane
	         // We clear it first just in case, then add the new shapes
	         this.lanePane.getChildren().clear();
	         this.lanePane.getChildren().add(lanesGroup);
	         
	         //Center the map
//	         Platform.runLater(() -> {
//	        	    // Calculate center based on the actual content size vs viewport size
//	        	    
//	        	    // Center the view (0.5 is the midpoint)
//	        	    mapScrollPane.setHvalue(0.5); // (hMin + hMax) * 0.5
//	        	    mapScrollPane.setVvalue(0.5); // (vMin + vMax) * 0.5
//	        	    
//	        	    // Optional: If you want to center on a specific coordinate (like the map center)
//	        	    // double contentWidth = mapContentGroup.getBoundsInLocal().getWidth();
//	        	    // mapScrollPane.setHvalue(0.5); 
//	        	});
	
	         log("Static Map drawn with " + lanesGroup.getChildren().size() + " lanes.");
            
            
            
            // --- B. START THREAD 2: SIMULATION ENGINE ---
            threadPool.submit(() -> {
                log("Simulation Thread Started.");
                while (isSimulationRunning) {
                    try {
                        // 1. Step physics (Thread-Safe)
                        simManager.step(); 
                        
                        // 2. Throttle speed (e.g., 100ms per step)
                        Thread.sleep(100); 
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break; 
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            // --- C. START THREAD 3: STATISTICS ---
            threadPool.submit(() -> {
                log("Stats Thread Started.");
                while (isSimulationRunning) {
                    try {
                        // Calculate stats less frequently (every 1 second)
                        Thread.sleep(1000);
                        
                        // Example: Log average speed
                        // System.out.println("Avg Speed: " + simManager.getStatisticsManager().getAverageSpeed());
                        
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });

            // --- D. START THREAD 1: UI RENDERING ---
            startUiLoop();
            
            startButton.setDisable(true); // Prevent double start
        } else {
            log("Failed to connect to SUMO.");
        }
    }
    
    /**
     * Starts the JavaFX AnimationTimer (60 FPS) to draw vehicles.
     */
    private void startUiLoop() {
        uiLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // This runs on the JavaFX UI Thread
            	/*
 * handle(long now): 
 * This method is called automatically by JavaFX roughly 60 times per second (depending on your monitor's refresh rate).

Why it's special: Code running inside handle() is executed on the JavaFX Application Thread. 
This is the only thread allowed to modify UI elements (like moving a Circle or changing a Label text).
            	 */
                updateView();
            }
        };
        uiLoop.start();
    }

    /**
     * Updates the visual elements based on the latest Model snapshot.
     */
    private void updateView() {
    	
    	
        // 1. Get Thread-Safe Snapshot (No manual synchronized needed here!)
//        var vehicles = simManager.getActiveVehicles();
//        int step = simManager.getCurrentStep();
//        
//        // 2. Update Labels
//        simStepLabel.setText(String.valueOf(step));
//        vehicleCountLabel.setText(String.valueOf(vehicles.size()));
        
        // 3. Update Dynamic Vehicle Shapes
//        updateVehicleShapes(vehicles);
    }
    
//    private void updateVehicleShapes(List<SumoVehicle> vehicles) {
//        // List of IDs present in this snapshot
//        List<String> currentIds = new ArrayList<>();
//
//        for (SumoVehicle v : vehicles) {
//            currentIds.add(v.getId());
//
//            // A. Create Shape if new
//            if (!vehicleVisuals.containsKey(v.getId())) {
//                // Note: createVehicleShape only needs the data object, not the connection
//                Shape s = renderer.createVehicleShape(null, v); // Connection not needed for simple shape logic
//                vehicleVisuals.put(v.getId(), s);
//                carPane.getChildren().add(s); // Add to pane
//            }
//
//            // B. Move Shape
//            Shape s = vehicleVisuals.get(v.getId());
//            if (v.getPosition() != null) {
//                // Convert Logic Coordinates -> Screen Coordinates
//                double screenX = converter.transformX(v.getPosition().x);
//                double screenY = converter.transformY(v.getPosition().y);
//                
//                s.setLayoutX(screenX);
//                s.setLayoutY(screenY);
//                // s.setRotate(v.getAngle()); // Optional rotation
//            }
//        }
//
//        // C. Cleanup (Remove shapes for vehicles that left)
//        // We iterate via copy to avoid concurrent mod exception on the visuals map
//        new ArrayList<>(vehicleVisuals.keySet()).forEach(id -> {
//            if (!currentIds.contains(id)) {
//                Shape s = vehicleVisuals.remove(id);
//                carPane.getChildren().remove(s);
//            }
//        });
//    }
    
    // --- HELPER: Logging ---
    private void log(String message) {
        System.out.println(message);
        if (logLabel != null) {
            // Ensure UI update happens on UI thread (important if called from background threads)
            Platform.runLater(() -> logLabel.setText(message + "\n" + logLabel.getText()));
        }
    }


    public void stopSimulation() {
        System.out.println("Stopping simulation...");
        
        // 1. Stop the loops
        isSimulationRunning = false;
        
        // 2. Stop the UI timer
        if (uiLoop != null) {
            uiLoop.stop();
        }
        
        // 3. KILL the background threads immediately
        if (threadPool != null) {
            threadPool.shutdownNow(); // This sends an "interruption" to the sleeps
        }
        
        // 4. Close connection
        if (simManager != null) {
            simManager.stopSimulation();
        }
    }

    // --- Placeholder Action Methods ---
    @FXML private void pauseSimulation() {
        // Toggle flag, handle pause logic
    }
    @FXML private void stepSimulation() {}
    @FXML private void injectVehicle() {}
    @FXML private void startSumoGUI() {}
    @FXML private void insertSumoConfigFile() {}
    @FXML private void applyFilter() {}
    @FXML private void clearFilter() {}
    @FXML private void runStressTest() {}
}