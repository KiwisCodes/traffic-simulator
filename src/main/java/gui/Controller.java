package gui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import logic.SimulationManager;

import java.net.URL;
import java.util.ResourceBundle;
import de.tudresden.sumo.cmd.Simulation;   // Để lấy thời gian
import de.tudresden.sumo.cmd.Trafficlight; // Để điều khiển đèn
import de.tudresden.sumo.objects.SumoStringList; // Để chứa danh sách ID

public class Controller implements Initializable {

    // ========================================================
    // 1. KHAI BÁO CÁC THÀNH PHẦN GIAO DIỆN (FXML)
    // ========================================================
    
    // Khu vực bản đồ
	@FXML private StackPane mapContainer; // fx:id="mapContainer"

    // Khu vực điều khiển chính
    @FXML private Button btnConnect; //@FXML: Là cách để code Java "nhìn thấy" các nút bạn vẽ bên Scene Builder. Nếu bên kia fx:id là btnStart thì bên này biến cũng phải tên là btnStart.
    @FXML private Button btnStart;
    @FXML private Button btnStop;
    
    // Khu vực thông tin trạng thái
    @FXML private Label statusLabel;
    @FXML private Label lblTime;

    // Khu vực Tab Vehicles (Xe)
    @FXML private TextField txtEdgeId;
    @FXML private Button btnAddVehicle;

    // Khu vực Tab Traffic Lights (Đèn)
    @FXML private ListView<String> listTrafficLights;
    @FXML private Button btnPhaseRed;
    @FXML private Button btnPhaseYellow;
    @FXML private Button btnPhaseGreen;
    
    @FXML private javafx.scene.layout.AnchorPane mapViewPort; // Khung bao bên ngoài
    

    // ========================================================
    // 2. CÁC BIẾN QUẢN LÝ LOGIC
    // ========================================================
    //  Thay vì viết tất cả code kết nối SUMO vào đây (làm Lễ tân bị quá tải), chúng ta thuê một trợ lý chuyên làm việc đó
    // Đây là những "Trợ lý chuyên môn" (OOP Objects)
    private SimulationManager simulationManager; // Quản lý kết nối SUMO
    private MapRenderer mapRenderer;             // Quản lý vẽ bản đồ
    private Thread simulationThread;             // Luồng chạy mô phỏng (Bắt buộc có 2 thread)
    private volatile boolean isRunning = false;  // Cờ kiểm soát vòng lặp

    // ========================================================
    // 3. KHỞI TẠO (INITIALIZE)
    // ========================================================
    
    @Override // Hàm này tự động chạy ngay khi cửa sổ phần mềm hiện lên.
    public void initialize(URL location, ResourceBundle resources) {
    	// Tuyển dụng trợ lý Manager
        simulationManager = new SimulationManager();
        
        // Cập nhật trạng thái ban đầu
        statusLabel.setText("Status: Ready to connect");
        lblTime.setText("Simulation Time: 0");
        
        // Khóa các nút chưa dùng được
        btnStart.setDisable(true); // Khóa nút Start (chưa kết nối thì chưa được chạy)
        btnStop.setDisable(true);
    }

    // ========================================================
    // 4. XỬ LÝ KẾT NỐI (CONNECT)
    // ========================================================
   
    // Hàm handle Connect Button (Khi khách bấm nút Kết nối)
    @FXML
    public void handleConnectButton() {
    	if (btnConnect.isDisabled()) { 
            return; 
        }
        btnConnect.setDisable(true); // Khóa nút ngay lập tức
        // -----------------------------------------------------------

        System.out.println("DEBUG: Bắt đầu quy trình kết nối..."); 
        statusLabel.setText("Status: Launching SUMO...");
        

        // 2. Tạo luồng riêng để đi kết nối (Không làm đơ giao diện)
        Thread connectionThread = new Thread(() -> {
            try {
                // --- ĐÂY LÀ VIỆC NẶNG NHẤT (Chạy ngầm) ---
                // Thiết lập biến môi trường cho Mac (Fix lỗi thư viện)
                System.setProperty("java.library.path", "/opt/homebrew/share/sumo/bin");
                
                // Gọi lệnh kết nối (Có thể mất 10-20s)
                simulationManager.connect();
                System.out.println("DEBUG: Đã kết nối thành công với SUMO!");

                // 3. Sau khi kết nối xong, quay lại luồng chính để vẽ vời
                Platform.runLater(() -> {
                    try {
                        statusLabel.setText("Status: Loading Map...");
                        
                        System.out.println("DEBUG: >> 1. Đang khởi tạo MapRenderer...");
                        mapRenderer = new MapRenderer(mapViewPort, mapContainer, simulationManager.getConnection());
                        
                        System.out.println("DEBUG: >> 2. Đang tính tỉ lệ bản đồ...");
                        mapRenderer.calculateScale();
                        
                        System.out.println("DEBUG: >> 3. Đang bắt đầu vẽ đường...");
                        mapRenderer.drawRoads();
                        
                        System.out.println("DEBUG: >> 4. Đang tải danh sách đèn...");
                        loadTrafficLightsList();

                        System.out.println("DEBUG: >> 5. HOÀN TẤT!");
                        statusLabel.setText("Status: Connected & Ready!");
                        btnStart.setDisable(false);
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                        statusLabel.setText("Status: Map Error!");
                    }
                });

            } catch (Exception e) {
                e.printStackTrace(); // In lỗi ra console để xem
                
                // Báo lỗi lên giao diện nếu kết nối thất bại
                Platform.runLater(() -> {
                    statusLabel.setText("Status: Connection Failed!");
                    btnConnect.setDisable(false); // Mở lại nút để thử lại
                    showAlert("Connection Error", "Không thể kết nối SUMO.\nLỗi: " + e.getMessage());
                });
            }
        });

        // Kích hoạt luồng
        connectionThread.setDaemon(true);
        connectionThread.start();
    }

    // ========================================================
    // 5. XỬ LÝ MÔ PHỎNG (START / STOP)
    // ========================================================

    @FXML
    public void handleStartButton() {
        if (isRunning) return;

        isRunning = true;
        btnStart.setDisable(true);
        btnStop.setDisable(false);
        statusLabel.setText("Status: Simulation Running...");

        // TẠO THREAD MỚI: Yêu cầu bắt buộc của đồ án 
        // Vòng lặp mô phỏng không được chạy trên main UI thread
        simulationThread = new Thread(() -> {
            while (isRunning) {
                try {
                    // A. Bước mô phỏng vật lý (Logic)
                    simulationManager.nextStep();

                    // B. Cập nhật giao diện (UI)
                    // Bất cứ thay đổi nào lên GUI phải dùng Platform.runLater
                    Platform.runLater(() -> {
                        // Cập nhật thời gian
                        double time = 0;
                        try {
                        	time = (double) simulationManager.getConnection().do_job_get(Simulation.getCurrentTime());
                        } catch (Exception e) { e.printStackTrace(); }
                        lblTime.setText("Simulation Time: " + time);

                        if (mapRenderer != null) {
                            mapRenderer.updateVehicles();
                        }
                    });

                    // C. Độ trễ để mắt người kịp nhìn (Animation speed)
                    Thread.sleep(100); // 100ms mỗi frame

                } catch (Exception e) {
                    e.printStackTrace();
                    isRunning = false; // Dừng nếu có lỗi
                }
            }
        });

        simulationThread.setDaemon(true); // Tự tắt khi tắt app
        simulationThread.start();
    }

    @FXML
    public void handleStopButton() {
        isRunning = false;
        statusLabel.setText("Status: Paused");
        btnStart.setDisable(false);
        btnStop.setDisable(true);
    }

    // ========================================================
    // 6. XỬ LÝ ĐÈN GIAO THÔNG (TRAFFIC LIGHTS)
    // ========================================================

    private void loadTrafficLightsList() {
        try {
            // Gửi lệnh lấy danh sách ID đèn giao thông
            SumoStringList idList = (SumoStringList) simulationManager.getConnection().do_job_get(Trafficlight.getIDList());
            
            // Chuyển đổi từ SumoStringList sang danh sách hiển thị lên màn hình
            ObservableList<String> items = FXCollections.observableArrayList(idList);
            listTrafficLights.setItems(items);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleRedPhase() {
        changeTrafficLightState("r"); // 'r' là mã màu đỏ trong SUMO
    }

    @FXML
    public void handleYellowPhase() {
        changeTrafficLightState("y"); // 'y' là mã màu vàng
    }

    @FXML
    public void handleGreenPhase() {
        changeTrafficLightState("g"); // 'g' là mã màu xanh (hoặc 'G' cho ưu tiên)
    }

    private void changeTrafficLightState(String stateCode) {
        String selectedId = listTrafficLights.getSelectionModel().getSelectedItem();
        if (selectedId == null) {
            showAlert("Warning", "Please select a Traffic Light ID from the list first.");
            return;
        }

        try {
            // --- CODE MỚI (SỬA LẠI) ---
            // Gửi lệnh setPhase thông qua do_job_set
            // Ở đây giả sử ta chuyển về Phase 0 (thường là Đỏ hoặc Xanh mặc định)
            // Bạn có thể tùy chỉnh logic này sau
            simulationManager.getConnection().do_job_set(Trafficlight.setPhase(selectedId, 0));
            
            System.out.println("Changed Light " + selectedId + " to phase 0");
            statusLabel.setText("Light " + selectedId + " switched!");
            
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Error changing light.");
        }
    }

    // ========================================================
    // 7. XỬ LÝ XE (VEHICLES)
    // ========================================================

    @FXML
    public void handleAddVehicle() {
        String edgeId = txtEdgeId.getText();
        if (edgeId.isEmpty()) {
            showAlert("Warning", "Please enter an Edge ID.");
            return;
        }

        try {
            // Logic thêm xe: Gọi Manager
            // Bạn cần viết hàm addVehicle trong SimulationManager sau
            // simulationManager.addVehicle(edgeId);
            
            statusLabel.setText("Vehicle added on " + edgeId);
        } catch (Exception e) {
            statusLabel.setText("Error adding vehicle. Invalid Edge ID?");
        }
    }

    // ========================================================
    // 8. TIỆN ÍCH (HELPER METHODS)
    // ========================================================

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}