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
import javafx.geometry.Pos; // import để căn giữa map

// Model & View Imports
import model.SimulationManager;
import model.infrastructure.MapManger;
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

//--- PHẦN TẠO DỮ LIỆU GIẢ (MOCK DATA) ---
// Import các thư viện này
import de.tudresden.sumo.objects.SumoColor;
import java.util.HashMap;
import de.tudresden.ws.container.SumoPosition2D; // Sửa lỗi SumoPosition2D
import de.tudresden.sumo.objects.SumoColor;     // Sửa lỗi SumoColor (chắc chắn bạn sẽ bị tiếp theo)

// Thêm để vẽ xe chuyển 
import javafx.animation.AnimationTimer;
import data.SimulationState;

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
 // --- THÊM DÒNG NÀY ---
    private MapManger mapManager; // Biến toàn cục để dùng ở mọi nơi
    // ---------------------
    @FXML private Pane baseMapPane;
    @FXML private Pane lanePane;     // Static roads go here
    @FXML private Pane junctionPane;
    @FXML private Pane trafficLightPane; // add vào để vẽ đèn giao thông
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
            this.mapManager = this.simManager.getMapManager(); // ✅ ĐÚNG: Lưu vào biến toàn cục 
            this.renderer.setConverter(this.mapManager); // Mới
            this.converter = this.renderer.getConverter();
            
            
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
                // Các lệnh setText này đều chỉ chạy khi lệnh accept() trong Renderer.java được kích hoạt.
            };
            
            /*
A Consumer<String> is a Java concept (introduced in Java 8) that lets you pass a block of code as if it were a variable.

Think of it as a "Task" or a "Job Order".



The Data: It expects one input (in this case, a String, which is your Lane ID).

The Result: It returns nothing (void). It just "consumes" the data and does something with it.
             */

            // Pass this action to the renderer
            Group lanesGroup = this.renderer.createLaneGroup(
                mapManager, 
                this.simManager.getConnection(),
                laneClickHandler // <--- Passing the function here
            );
	         
	
	         // 4. Add the lanes to the GUI Pane
	         // We clear it first just in case, then add the new shapes
	         try {
				this.lanePane.getChildren().clear();
				 this.lanePane.getChildren().add(lanesGroup);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	         
	      // A. Tạo hành động: Khi click vào đèn thì làm gì?
	         Consumer<String> trafficLightClickHandler = (tlId) -> {
	             // Điền ID vào ô nhập liệu "trafficLightIdField" trên giao diện
	             trafficLightIdField.setText(tlId);
	             log("Đã chọn đèn giao thông: " + tlId);
	         };

	         // B. Gọi Renderer vẽ nhóm đèn
	         Group tlGroup = this.renderer.createTrafficLightGroup(
	             this.simManager.getConnection(), 
	             trafficLightClickHandler // Truyền hành động vào
	         );

	         // C. Đưa lên màn hình (trafficLightPane đã có sẵn trong FXML của bạn)
	         this.trafficLightPane.getChildren().clear();
	         this.trafficLightPane.getChildren().add(tlGroup);
	         
	         log("Đã vẽ xong " + tlGroup.getChildren().size() + " đèn giao thông.");
	         // ------------------------------------------
	         
	         // ...
	         
//	         centerAndFitMap();
	         
	         
//	         Rectangle clipRect = new Rectangle();
//	         clipRect.widthProperty().bind(rightMapStackPane.widthProperty());
//	         clipRect.heightProperty().bind(rightMapStackPane.heightProperty());
//	         rightMapStackPane.setClip(clipRect);
	      // --- FIX 2: Force Z-Order (Safety measure) ---
	         // This guarantees the sidebar is drawn last (on top)
	         topHbox.toFront();
	         leftControlPanel.toFront();
//	         // If you have a log area at the bottom, bring that to front too
	          bottomLogArea.toFront();
	          
	         
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
        
        //startUiLoop();
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
        log("Đã khởi động Animation Loop.");
    }

    /**
     * Updates the visual elements based on the latest Model snapshot.
     */
    private void updateView() {
    	System.out.println("UpdateView đang chạy...");
        // --- CÁCH CŨ (Đang chờ đồng đội) ---
        /* if (simQueue == null) return;
        SimulationState state = simQueue.pollState();
        if (state != null) {
             this.renderer.renderVehicles(this.vehiclePane, state.getVehicles());
        }
        */

        // --- CÁCH MỚI (TEST RENDERER) ---
        
        // 1. Tự sinh dữ liệu giả
//        Map<String, Map<String, Object>> fakeData = generateFakeVehicleData();
//    	this.simManager.getVehicleManager().step();
//    	this.simManager.getVehicleManager().updateVehiclesInfo();
//    	Map<String, Map<String, Object>> realData = this.simManager.getVehicleManager().getVehiclesData();

        // 2. Gọi Renderer vẽ ngay lập tức
        this.renderer.renderVehicles(this.vehiclePane, realData);
        
        // (Tùy chọn) In ra console để biết nó đang chạy
        // System.out.println("Đang vẽ frame giả thứ: " + dummyStep);
    }
    
    // --- HELPER: Logging ---
    private void log(String message) {
        System.out.println(message);
        if (logLabel != null) {
            // Ensure UI u	pdate happens on UI thread (important if called from background threads)
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
    
    private long dummyStep = 0; // Biến đếm để tạo chuyển động

    private Map<String, Map<String, Object>> generateFakeVehicleData() {
        Map<String, Map<String, Object>> allVehicles = new HashMap<>();
        
        double baseX = 0;
        double baseY = 0;

        // 1. [QUAN TRỌNG] Lấy tọa độ gốc của bản đồ thật
        if (this.mapManager != null) {
            baseX = this.mapManager.getMinX();
            baseY = this.mapManager.getMinY();
            // System.out.println("Gốc bản đồ thật tại: " + baseX + ", " + baseY);
        }

        // --- XE 1 (Đỏ) ---
        Map<String, Object> car1 = new HashMap<>();
        
        // 2. CỘNG BASEX VÀO ĐỂ XE NHẢY VÀO TRONG MAP
        double x1 = baseX + 100 + (dummyStep * 5) % 1000; 
        double y1 = baseY + 200; 
        
        SumoPosition2D pos1 = new SumoPosition2D();
        pos1.x = x1;
        pos1.y = y1;
        
        car1.put("Position", pos1);
        car1.put("Color", new SumoColor(255, 0, 0, 255)); 
        car1.put("Angle", 90.0);
        
        allVehicles.put("fake_car_red", car1);

        // --- XE 2 (Xanh) ---
        Map<String, Object> car2 = new HashMap<>();
        double x2 = baseX + 500;
        double y2 = baseY + 100 + (dummyStep * 5) % 800;
        
        SumoPosition2D pos2 = new SumoPosition2D();
        pos2.x = x2;
        pos2.y = y2;
        
        car2.put("Position", pos2);
        car2.put("Color", new SumoColor(0, 255, 0, 255));
        car2.put("Angle", 180.0);
        
        allVehicles.put("fake_car_green", car2);
        
        dummyStep++;
        return allVehicles;
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