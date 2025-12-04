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
    @FXML private TitledPane injectionPane;       // Khung chứa chức năng thêm xe
    @FXML private RadioButton carRadio;           // Nút chọn Ô tô
    @FXML private RadioButton bikeRadio;          // Nút chọn Xe đạp
    @FXML private ToggleGroup vehicleTypeGroup;   // Nhóm nút chọn (để biết cái nào đang active)
    @FXML private TextField firstEdgeField;       // Ô chứa ID điểm xuất phát
    @FXML private TextField secondEdgeField;      // Ô chứa ID điểm đích
   

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
    @FXML private TextField pathToSumocfgFileField;
    @FXML private TextField pathToSumoGuiField; 
    @FXML private Button loadSumoPathButton;
    @FXML private Button loadSumoConfigButton;

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
    @FXML private AnchorPane centerMapAnchorPane;
    @FXML private StackPane centerMapStackPane;
    @FXML private Group centerMapPaneGroup;
    @FXML private Pane vehiclePane;
    @FXML private Pane carLanePane;      // Pane chứa đường ô tô
    @FXML private Pane bikeLanePane;     // Pane chứa đường xe đạp
    @FXML private ScrollPane bottomLogScrollPane;
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


//    Logic & State
    private SimulationManager simManager;
    private Renderer renderer; 
   
//    Thread
    private AnimationTimer uiLoop; 
    private ExecutorService threadPool; 
    private final int NUMBER_OF_THREADS = 2; 
    
//    Flags
    private volatile static boolean isSimulationRunning = false;
    private volatile static int currentStep = 0;
    private volatile static boolean isPaused = false;

    // --- Visualization ---
    // Map to track visual shapes: ID -> Shape (Used to update positions)
    private Map<String, Shape> vehicleVisuals = new HashMap<>();
    private Group mapContentGroup; // Container for zooming/panning
    private MapInteractionHandler mapInteractionHandler;
    private SimulationQueue queue;	

//    private final double PADDING = 50.0;//we dont need this
    
    
    // --- Initialization ---

    public MainController() {
    	this.queue = new SimulationQueue(1000);
        this.simManager = new SimulationManager(queue);
        this.renderer = new Renderer();
        this.threadPool = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
        this.queue = new SimulationQueue(1000);
    }
    
    // Main entry point if running stand alone (optional)
    public static void main(String[] args) {
        // JavaFX launching logic usually goes in MainGUI.java
    }

    @FXML
    public void initialize() {
    	
        log("Controller initialized. Waiting to start...");
        this.mapInteractionHandler = new MapInteractionHandler(centerMapStackPane, centerMapPaneGroup);
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
        
        disableButtons(true);
    }

    @FXML 
    private void startSimulation() {
        this.startButton.setDisable(true); // Prevent double start
        log("Attempting to connect to SUMO...");
        boolean connected = this.simManager.startConnection();

        if (connected) {
            log("Connected! Preparing simulation...");
            isSimulationRunning = true;
            disableButtons(false);
            MapManager mapManager = this.simManager.getMapManager();
            this.renderer.setConverter(mapManager);

            Consumer<String> laneClickHandler = (laneId) -> {
            	String edgeId = laneId.substring(0,laneId.indexOf("_"));                
            	//we derive the edgeId from the the laneId
            	//ex: laneId: 3242345_234 -> we just want the things before _ so we find the index of _ and take the substring before it
                // 1. Kiểm tra xem Menu thêm xe có đang mở không?
                if (injectionPane != null && injectionPane.isExpanded()) {
                    // 2. Logic điền lần lượt: Điền ô 1 -> Điền ô 2 -> Reset quay lại ô 1
                    if (firstEdgeField.getText().isEmpty()) {
                        firstEdgeField.setText(edgeId);
                        log("Selected First Edge: " + edgeId);
                        
                    } else if (secondEdgeField.getText().isEmpty()) {
                        secondEdgeField.setText(edgeId);
                        log("Selected Second Edge: " + edgeId);
                        
                    } else {
                        // Nếu cả 2 ô đã có dữ liệu, click lần nữa sẽ reset ô 1 thành đường mới chọn
                        firstEdgeField.setText(edgeId);
                        secondEdgeField.clear();
                        log("Selected Another First Edge  " + edgeId);
                    }
                    
                } else {
                    // Nếu menu đang đóng thì chỉ in log xem chơi
                    log("Edge ID: " + edgeId); 
                }
            };
            
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
	         log("Static Map drawn (Separated Car/Bike lanes)");
            

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
                	if(isPaused) {
            			try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();//this is needed when user hit stop when it is currently paused
							e.printStackTrace();
						}
            			continue;
                	}
                    try {
                        // 1. Step physics (Thread-Safe)
//                    	if(!injected.get()) {
//    						simManager.InjectVehicle( "DEFAULT_VEHTYPE", 255, 255, 255, 0, 3.6, "66993637#0", "265499402#5");
//    						simManager.InjectVehicle( "DEFAULT_VEHTYPE", 255, 255, 255, 0, 3.6, "9792393#0", "98428996#3");
//    						simManager.InjectVehicle( "DEFAULT_VEHTYPE", 255, 255, 255, 0, 3.6, "627278688", "676073620#0");
//    						simManager.InjectVehicle( "DEFAULT_VEHTYPE", 255, 255, 255, 0, 3.6, "66993637#0", "265499402#5");
//    						injected.set(true);
//    					}
                        this.simManager.step();
//                        simManager.StressTest();
                        this.queue.offerState(this.simManager.getState());// by this we dont get interrupted, unlike putState
                        currentStep++;
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
        } else {
            log("Failed to connect to SUMO.");
        }
    }
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

    private void updateView() {

    	SimulationState simulationState;
		try {
			simulationState = this.queue.pollState();
			if(simulationState == null) return;
			
			this.renderer.renderVehicles(vehiclePane, simulationState.getVehicles());
			int currentVehicleCount = simulationState.getVehicles().size();
			updateCurrentStep();
			updateCurrentVehicleCount(currentVehicleCount);
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.err.print(e.getMessage());
		}
    }
    
    // --- HELPER: Logging ---
    private void log(String message) {
        System.out.println(message);
        
        // Check if the label exists before attempting UI updates
        if (logLabel != null) {
            
            // Ensure all UI updates happen on the JavaFX Application Thread
            Platform.runLater(() -> {
                
                // 1. Append the new message to the existing log text
                logLabel.setText(message + "\n" + logLabel.getText());
                
                // 2. Set the vertical scroll value to 1.0 (the bottom)
                // We must check if the ScrollPane exists before setting the value
                if (this.bottomLogScrollPane != null) {
                    this.bottomLogScrollPane.setVvalue(1.0);
                }
            });
        }
    }
    
    private void updateCurrentStep() {
    	System.out.println(currentStep);
    	if(simStepLabel != null) {
    		simStepLabel.setText("" + currentStep);
    	}
    }
    
    private void updateCurrentVehicleCount(int currentVehicleCount) {
    	System.out.println(currentVehicleCount);
    	if(vehicleCountLabel != null) {
    		vehicleCountLabel.setText("" + currentVehicleCount);
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
    
    private void disableButtons(boolean state) {
    	this.pauseButton.setDisable(state);
        this.stepButton.setDisable(state);
        this.injectVehicleButton.setDisable(state);
        this.showChartsButton.setDisable(state);
        this.exportCsvButton.setDisable(state);
        this.stressTestButton.setDisable(state);
    }

    @FXML private void pauseSimulation() {
    	if(pauseButton == null) return;
    	isPaused = !isPaused;
    	if(isPaused == true) {
    		log("Simulation Paused");
			pauseButton.setText("Resume");
    	}
    	else {
    		log("Resume Simulation");
    		pauseButton.setText("⏸ Pause");
    	}
    }
    @FXML private void stepSimulation() {
    	if(!isPaused) {
    		log("Please pause the simulation first");
    		return;
    	}
    	
    	this.simManager.step();
    	currentStep++;
    	SimulationState newSimulationState = this.simManager.getState();
    	try {
			this.queue.offerState(newSimulationState);
			log("Step Forward -> " + currentStep);
		} catch (InterruptedException e) {
			log("Error stepping: " + e.getMessage());
			e.printStackTrace();
		}
    	
    }
    
    @FXML private void loadSumoPath() {
    	if(this.simManager.setSumoBinary(this.pathToSumoGuiField)) {
    		log("Successfully set path to sumo or sumo-gui");
    		this.loadSumoPathButton.setDisable(true);
    	}
    	else {
    		log("Set sumo or sumo-gui path fail");
    	}
    }
    
    @FXML private void injectVehicle() {
    	if(this.firstEdgeField.getText().isEmpty() || this.secondEdgeField.getText().isEmpty()) {
    		log("Please choose 2 edges please");
    		return;
    	}
    	
    	String firstEdgeId = this.firstEdgeField.getText();
    	String secondEdgeId = this.secondEdgeField.getText();
    	String vehicleType = null;
    	if(this.carRadio.isSelected()) vehicleType = "DEFAULT_VEHTYPE";
    	else if(this.bikeRadio.isSelected()) vehicleType = "DEFAULT_BIKETYPE";
    	this.simManager.InjectVehicle(vehicleType, 255, 255, 255, 1, 3.6, firstEdgeId, secondEdgeId);
    	log("Injected vehicle");
    }
    @FXML private void startSumoGUI() {}
    @FXML private void insertSumoConfigFile() {}
    @FXML private void applyFilter() {}
    @FXML private void clearFilter() {}
    @FXML private void runStressTest() {
    	try {
			this.simManager.StressTest();
			log("Default Stress Test with 50 random cars with random Routes");
		} catch (Exception e) {
			System.err.print(e.getMessage());
			e.printStackTrace();
		}
    }
    
    
    
    
}