package controller;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.transform.Scale;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

// Model & View Imports
import model.SimulationManager;
import model.infrastructure.MapManager;
import model.vehicles.VehicleManager;
import view.Renderer;
import util.CoordinateConverter; // Ensure this is imported from your util/view package

// Java Imports
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import data.SimulationQueue;

public class MainController {
	
	@FXML private HBox topHbox;

    // --- FXML View Elements ---
    @FXML private ScrollPane leftControlPanel;
//    @FXML private ScrollPane mapScrollPane;
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
//    @FXML private AnchorPane rightMapAnchorPane;
    @FXML private AnchorPane mapAnchorPane;
    @FXML private StackPane rightMapStackPane;
    @FXML private Group rightMapPaneGroup;
    @FXML private Pane vehiclePane;
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
    @FXML private TitledPane bottomLogArea;

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
    private MapInteractionHandler mapInteractionHandler;
    private SimulationQueue queue;	

    // Scaling constants
    private final double PADDING = 50.0;
    
    // --- Initialization ---

    public MainController() {
        // 1. Initialize Model
    	this.queue = new SimulationQueue(1000);
        this.simManager = new SimulationManager(queue); // Logic Engine
        
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
        this.mapInteractionHandler = new MapInteractionHandler(rightMapStackPane, rightMapPaneGroup);
        
//        Rectangle clipRect = new Rectangle();
//
//        // 2. Bind the clip to the StackPane (which acts as the "Center" window)
//        clipRect.widthProperty().bind(rightMapStackPane.widthProperty());
//        clipRect.heightProperty().bind(rightMapStackPane.heightProperty());
//
//        // 3. Apply the clip
//        rightMapStackPane.setClip(clipRect);
        
        
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
            MapManager mapManager = this.simManager.getMapManager();
            this.renderer.setConverter(mapManager);
            this.converter = this.renderer.getConverter();
            this.mapInteractionHandler.centerMap();
            
            
//         // 2. --- CRITICAL FIX --- 
//            // Calculate the correct scale based on the View Pane's current size
//            double availableWidth = rightMapStackPane.getWidth();
//            double availableHeight = rightMapStackPane.getHeight();
//            
//            // Safety check: if width/height is 0 (scene not loaded yet), default to something reasonable
//            if (availableWidth == 0) availableWidth = 800;
//            if (availableHeight == 0) availableHeight = 600;
//
//            // Calculate scale inside the converter
//            this.converter.autoFit(availableWidth, availableHeight);
//            
//         // --- DEBUG PRINT START ---
//            System.out.println("--- MAP DEBUG INFO ---");
//            System.out.println("Pane Size: " + availableWidth + " x " + availableHeight);
//            System.out.println("Map Scale: " + this.converter.getScale());
//            System.out.println("Map Offset: X=" + this.converter.toScreenX(0) + " Y=" + this.converter.toScreenY(0));
            // --- DEBUG PRINT END ---
            
            
            //insde the renderer there is a converter, inside this converter there is the sumoMap info
            /*
             * The Lambda () -> { ... }: This is the code you want to run in the background. It's an anonymous function that defines the task.
             */
            //Draw just the lanes first
	         // DRAW LANES (The code you need)
	         // We ask the renderer to create the Group of lanes using the active connection
//	         Group lanesGroup = this.renderer.createLaneGroup(this.simManager.getConnection(), sumoMap);
            
         // Define the action (What happens when clicked?)
            Consumer<String> laneClickHandler = (laneId) -> {
                // Update your UI text fields
                routeIdField.setText(laneId); 
                stressEdgeField.setText(laneId);
                filterEdgeField.setText(laneId);
                log("User selected lane: " + laneId);
            };
            
            /*
A Consumer<String> is a Java concept (introduced in Java 8) that lets you pass a block of code as if it were a variable.

Think of it as a "Task" or a "Job Order".



The Data: It expects one input (in this case, a String, which is your Lane ID).

The Result: It returns nothing (void). It just "consumes" the data and does something with it.
             */

            // Pass this action to the renderer
            Group lanesGroup = this.renderer.createLaneGroup(
                this.simManager.getConnection(), 
                mapManager, 
                laneClickHandler // <--- Passing the function here
            );
	         
	
	         // 4. Add the lanes to the GUI Pane
	         // We clear it first just in case, then add the new shapes
	         this.lanePane.getChildren().clear();
	         this.lanePane.getChildren().add(lanesGroup);
	         
	         // This guarantees the sidebar is drawn last (on top)
	         topHbox.toFront();
	         leftControlPanel.toFront();
//	         // If you have a log area at the bottom, bring that to front too
	          bottomLogArea.toFront();
	          
	         
	
	         log("Static Map drawn with " + lanesGroup.getChildren().size() + " lanes.");
            
            
            
            // --- B. START THREAD 2: SIMULATION ENGINE ---
            threadPool.submit(() -> {
                log("Simulation Thread Started.");
                while (isSimulationRunning) {
                    try {
                        // 1. Step physics (Thread-Safe)
                        simManager.step(); 
                        
                        // 2. Throttle speed (e.g., 100ms per step)
//                        Thread.sleep(100); 
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            // --- C. START THREAD 3: STATISTICS ---
            threadPool.submit(() -> {
                log("Stats Thread Started.");
                while (isSimulationRunning) {
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
            	
//                updateView();
            	
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
    
    //this function does not work
//    private void centerAndFitMap() {
//        Platform.runLater(() -> {
//            // 1. Get the actual size of the Map Content
//            var mapBounds = rightMapPaneGroup.getLayoutBounds();
//            double mapWidth = mapBounds.getWidth();
//            double mapHeight = mapBounds.getHeight();
//            
//            // 2. Get the size of the Window (StackPane)
//            double windowWidth = rightMapStackPane.getWidth();
//            double windowHeight = rightMapStackPane.getHeight();
//
//            if (windowWidth == 0 || windowHeight == 0) return;
//
//            // 3. Calculate Scale to FIT
//            double scaleX = windowWidth / mapWidth;
//            double scaleY = windowHeight / mapHeight;
//            
//            // Use the smaller scale so it fits entirely (with 90% padding)
//            double scaleFactor = Math.min(scaleX, scaleY) * 0.90;
//
//            // 4. Calculate the Center of the Map (This is your Pivot)
//            double pivotX = mapBounds.getMinX() + (mapWidth / 2);
//            double pivotY = mapBounds.getMinY() + (mapHeight / 2);
//
//            // 5. Create the Scale Transform with the Pivot
//            // Constructor: Scale(x, y, pivotX, pivotY)
//            Scale scaleTransform = new Scale(scaleFactor, scaleFactor, pivotX, pivotY);
//
//            // 6. Apply the Transform
//            rightMapPaneGroup.getTransforms().clear(); // Clear previous zooms
//            rightMapPaneGroup.getTransforms().add(scaleTransform);
//            
//            // 7. Reset Translations (Let StackPane handle the centering)
//            // Because the StackPane automatically centers its children, and we just 
//            // scaled the child around its own center, it should snap perfectly to the middle.
//            rightMapPaneGroup.setTranslateX(0);
//            rightMapPaneGroup.setTranslateY(0);
//
//            log("Map Centered. Scale: " + String.format("%.4f", scaleFactor));
//        });
//    }

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