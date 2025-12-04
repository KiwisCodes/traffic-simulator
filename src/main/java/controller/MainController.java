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
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;

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
import java.util.concurrent.atomic.AtomicBoolean;
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
import data.SimulationState;

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
    private MapManager mapManager; // Biến toàn cục để dùng ở mọi nơi
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
    @FXML private Pane mixedLanePane; // <--- THÊM MỚI
    @FXML private Label logLabel;
    @FXML private Button zoomInButton;
    @FXML private Button zoomOutButton;
    @FXML private Button resetViewButton;
    @FXML private ToggleButton toggle3DButton;
    @FXML private TitledPane bottomLogArea;
 // --- KHAI BÁO MỚI CHO TÍNH NĂNG TÁCH LANE ---
    @FXML private Pane carLanePane;      // Pane chứa đường ô tô
    @FXML private Pane bikeLanePane;     // Pane chứa đường xe đạp
 // --- KHAI BÁO CHO GIAO DIỆN CHỌN XE ---
    @FXML private TitledPane injectionPane;       // Khung chứa chức năng thêm xe
    @FXML private RadioButton carRadio;           // Nút chọn Ô tô
    @FXML private RadioButton bikeRadio;          // Nút chọn Xe đạp
    @FXML private ToggleGroup vehicleTypeGroup;   // Nhóm nút chọn (để biết cái nào đang active)
    
    @FXML private TextField firstEdgeField;       // Ô chứa ID điểm xuất phát
    @FXML private TextField secondEdgeField;      // Ô chứa ID điểm đích

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
    private volatile static int currentStep = 0;

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
        
        // 4. Map related
        
        // 5. Data
        this.queue = new SimulationQueue(1000);
    }
    
    // Main entry point if running standalone (optional)
    public static void main(String[] args) {
        // JavaFX launching logic usually goes in MainGUI.java
    }

    @FXML
    public void initialize() {
    	
        log("Controller initialized. Waiting to start...");
        this.mapInteractionHandler = new MapInteractionHandler(rightMapStackPane, rightMapPaneGroup);
        if (injectionPane != null) {
            injectionPane.expandedProperty().addListener((obs, wasExpanded, isNowExpanded) -> {
            	InteractWithVehicleInjectionDropMenu(); // Cập nhật ngay lập tức
                
                // (Tùy chọn) Reset các ô text khi đóng lại cho sạch
                if (!isNowExpanded) {
                    if (firstEdgeField != null) firstEdgeField.clear();
                    if (secondEdgeField != null) secondEdgeField.clear();
                }
            });
        }
     // --- 2. Lắng nghe việc CHỌN LOẠI XE (Car/Bike) ---
        if (vehicleTypeGroup != null) {
            vehicleTypeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            	InteractWithVehicleInjectionDropMenu(); // Cập nhật ngay lập tức
            });
        }
        
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
            
            
            double viewWidth = rightMapStackPane.getWidth();
            double viewHeight = rightMapStackPane.getHeight();

            // Fallback: If the window just opened, size might be 0. Guess a size.
            if (viewWidth == 0) viewWidth = 1400;
            if (viewHeight == 0) viewHeight = 900;

            // This calculates Scale AND the Offset needed to center it
//            this.renderer.getConverter().autoFit(viewWidth, viewHeight);
            

            this.converter = this.renderer.getConverter();
//            this.mapInteractionHandler.centerMap(this.lanePane);
            
            
         // Define the action (What happens when clicked?)
Consumer<String> laneClickHandler = (laneId) -> {
                
                // 1. Kiểm tra xem Menu thêm xe có đang mở không?
                if (injectionPane != null && injectionPane.isExpanded()) {
                    
                    // 2. Logic điền lần lượt: Điền ô 1 -> Điền ô 2 -> Reset quay lại ô 1
                    if (firstEdgeField.getText().isEmpty()) {
                        firstEdgeField.setText(laneId);
                        log("Selected First Edge: " + laneId);
                        
                    } else if (secondEdgeField.getText().isEmpty()) {
                        secondEdgeField.setText(laneId);
                        log("Selected Second Edge: " + laneId);
                        
                    } else {
                        // Nếu cả 2 ô đã có dữ liệu, click lần nữa sẽ reset ô 1 thành đường mới chọn
                        firstEdgeField.setText(laneId);
                        secondEdgeField.clear();
                        log("🔄 Selected Another First Edge  " + laneId);
                    }
                    
                } else {
                    // Nếu menu đang đóng thì chỉ in log xem chơi
                    log("Lane ID: " + laneId); 
                }
            };
            
            /*
A Consumer<String> is a Java concept (introduced in Java 8) that lets you pass a block of code as if it were a variable.

Think of it as a "Task" or a "Job Order".



The Data: It expects one input (in this case, a String, which is your Lane ID).

The Result: It returns nothing (void). It just "consumes" the data and does something with it.
             */

            // Pass this action to the renderer
         // 1. Gọi hàm vẽ phân loại (Render trực tiếp vào 2 Pane)
            this.renderer.renderLanes(
                this.mapManager,                 // Dùng biến toàn cục này
                this.simManager.getConnection(), 
                this.carLanePane,                // Pane chứa đường ô tô
                this.bikeLanePane,               // Pane chứa đường xe đạp
                this.mixedLanePane,
                laneClickHandler                 // Hàm xử lý click
            );
	         
            // 2. Cài đặt trạng thái tương tác ban đầu
            // (Đảm bảo lúc mới Start, menu đang đóng thì cả 2 đường đều sáng/click được)
            InteractWithVehicleInjectionDropMenu();
	         
//	         this.mapInteractionHandler.centerMap(this.lanePane);// the java wait for 1 more frame before calculating the size of the lanePane, so init is 0x0
	         
//	         Platform.runLater(() -> {
//	        	 this.mapInteractionHandler.centerMap(lanePane);
//	        	 
//	         });
	         
	         // This guarantees the sidebar is drawn last (on top)
//	         topHbox.toFront();
//	         leftControlPanel.toFront();
//////	         // If you have a log area at the bottom, bring that to front too
//	          bottomLogArea.toFront();
	          
	         
	
	         log("Static Map drawn (Separated Car/Bike lanes).");
            

	         this.renderer.renderJunctions(
	        	        this.simManager.getConnection(), 
	        	        this.junctionPane,  // Truyền Pane vào cho Renderer tự vẽ
	        	        juncId -> log("Selected Junction: " + juncId)
	        	    );
            

	 		AtomicBoolean injected = new AtomicBoolean(false);
  

            // --- B. START THREAD 2: SIMULATION ENGINE ---
            threadPool.submit(() -> {
                log("Simulation Thread Started.");
                while (isSimulationRunning) {
                	
                	if(this.simManager.getConnection().isClosed()) {
                		log("Connection lost, stopping loop");
                		break;
                	}
                	
                    try {
                        // 1. Step physics (Thread-Safe)
                    	if(!injected.get()) {
    						simManager.InjectVehicle( "DEFAULT_VEHTYPE", 255, 255, 255, 0, 3.6, "66993637#0", "265499402#5");
    						simManager.InjectVehicle( "DEFAULT_VEHTYPE", 255, 255, 255, 0, 3.6, "9792393#0", "98428996#3");
    						simManager.InjectVehicle( "DEFAULT_VEHTYPE", 255, 255, 255, 0, 3.6, "627278688", "676073620#0");
    						simManager.InjectVehicle( "DEFAULT_VEHTYPE", 255, 255, 255, 0, 3.6, "66993637#0", "265499402#5");
    						injected.set(true);
    					}
                        this.simManager.step();
                        simManager.StressTest();

//                        this.queue.putState(this.simManager.getState());// when click stop, the queue is doing put, but interrupted -> error
                        this.queue.offerState(this.simManager.getState());// by this we dont get interrupted;
                        currentStep++;
                        log("Current Step: " + currentStep);
                        // 2. Throttle speed (e.g., 100ms per step)
//                        Thread.sleep(100); 
                        
                        if(this.simManager.getConnection().isClosed()) {
                        	log("Dead");
                        	break;
                        }
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
            	

                

                updateView();//maybe this should draw everything;

            	
            }
        };
        uiLoop.start();
        log("Đã khởi động Animation Loop.");
    }

    /**
     * Updates the visual elements based on the latest Model snapshot.
     */
    private void updateView() {

    	SimulationState simulationState;
		try {
			simulationState = this.queue.pollState();
			if(simulationState == null) return;
			log("Took state");
			this.renderer.renderVehicles(vehiclePane, simulationState.getVehicles());
//			this.vehiclePane.toFront();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    // --- HELPER: Logging ---
    private void log(String message) {
        System.out.println(message);
        if (logLabel != null) {
            // Ensure UI u	pdate happens on UI thread (important if called from background threads)
            Platform.runLater(() -> logLabel.setText(message + "\n" + logLabel.getText()));
        }
    }
    
    private void InteractWithVehicleInjectionDropMenu() {
        // TRƯỜNG HỢP 1: Nếu Menu "Vehicle Injection" đang ĐÓNG
        // -> Cho phép tương tác với TẤT CẢ (để người dùng soi map)
        if (injectionPane == null || !injectionPane.isExpanded()) {
            if (carLanePane != null) carLanePane.setMouseTransparent(false);
            if (bikeLanePane != null) bikeLanePane.setMouseTransparent(false);
            if (mixedLanePane != null) mixedLanePane.setMouseTransparent(false);
            return;
        }

        // TRƯỜNG HỢP 2: Nếu Menu đang MỞ -> Kiểm tra loại xe
        boolean isBikeMode = bikeRadio.isSelected();

        if (isBikeMode) {
            // --- Đang chọn XE ĐẠP ---
            // Đường Ô tô: Tắt tương tác (Không sáng)
            if (carLanePane != null) carLanePane.setMouseTransparent(true); // tắt 
            // Đường Xe đạp: Bật tương tác (Sáng)
            if (bikeLanePane != null) bikeLanePane.setMouseTransparent(false); // bật xe đạp
            if (mixedLanePane != null) mixedLanePane.setMouseTransparent(false); // Bật Mixed
            
        } else {
            // --- Đang chọn Ô TÔ ---
            // Đường Ô tô: Bật tương tác (Sáng)
            if (carLanePane != null) carLanePane.setMouseTransparent(false); //Bật car 
            // Đường Xe đạp: Tắt tương tác (Không sáng)
            if (bikeLanePane != null) bikeLanePane.setMouseTransparent(true); //tắt bike
            if (mixedLanePane != null) mixedLanePane.setMouseTransparent(false); // Bật Mixed
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