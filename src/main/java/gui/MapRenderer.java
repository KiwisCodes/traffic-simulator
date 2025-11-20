package gui;

import de.tudresden.sumo.cmd.Lane;
import de.tudresden.sumo.cmd.Simulation;
import de.tudresden.sumo.cmd.Vehicle;
import de.tudresden.sumo.objects.SumoBoundingBox;
import de.tudresden.sumo.objects.SumoColor;
import de.tudresden.sumo.objects.SumoGeometry;
import de.tudresden.sumo.objects.SumoPosition2D;
import de.tudresden.sumo.objects.SumoStringList;
import it.polito.appeal.traci.SumoTraciConnection;
import javafx.application.Platform;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MapRenderer {

  
    private final SumoTraciConnection connection;

    // CÁC LỚP (LAYERS)
    private Pane roadLayer;         // Lớp 1: Đường
    private Pane trafficLightLayer; // Lớp 2: Đèn
    private Pane carLayer;          // Lớp 3: Xe con
    private Pane truckLayer;        // Lớp 4: Xe tải

    // Quản lý hình ảnh xe
    private Map<String, Shape> vehicleMap = new HashMap<>();

    // Biến tỉ lệ bản đồ
    private double scale = 1.0;
    private double minX = 0;
    private double minY = 0;
    private double mapHeight = 0;
    private double xOffset = 0;
    private double yOffset = 0;
    
    // Thêm biến lưu giới hạn bản đồ để dùng chung
    private double x_max = 0;
    private double y_max = 0;
    
    // BIẾN DÙNG CHO KÉO THẢ (PANNING)
    private double mouseAnchorX;
    private double mouseAnchorY;
    private double translateAnchorX;
    private double translateAnchorY;

    private final Pane mapViewPort;     // Khung nhìn (Cố định)
    private final StackPane mapContainer; // Nội dung bản đồ (Phóng to/Thu nhỏ)
    
    
    public MapRenderer(Pane mapViewPort, StackPane mapContainer, SumoTraciConnection connection) {
    	this.mapViewPort = mapViewPort;     // Lưu khung nhìn
        this.mapContainer = mapContainer;   // Lưu nội dung
        this.connection = connection;
        //1. Ép khung nhìn cố định (Viewport) phải luôn màu đen (#2b2b2b)
        this.mapViewPort.setStyle("-fx-background-color: #2b2b2b; -fx-border-width: 0;");
     // 2. Nội dung bên trong: Trong suốt, VÀ QUAN TRỌNG LÀ KHÔNG CÓ VIỀN
        // Thêm "-fx-border-width: 0;" để xóa cái khung chữ nhật đi
        this.mapContainer.setStyle("-fx-background-color: transparent; -fx-border-width: 0; -fx-border-color: transparent;");
       
        
        // -------------------------------------
        
        // Khởi tạo các lớp ngay khi tạo đối tượng
        initLayers();
     // GỌI HÀM TẠO MẶT NẠ CẮT
        setupClipping();
        enableZoomAndPan();
    }
    
    /**
     * Tạo mặt nạ (Clip) để bản đồ không bị tràn ra ngoài khung khi Zoom
     */
    private void setupClipping() {
        Rectangle clipRect = new Rectangle();
        
        // Ràng buộc kích thước mặt nạ theo kích thước của khung nhìn
        clipRect.widthProperty().bind(mapViewPort.widthProperty());
        clipRect.heightProperty().bind(mapViewPort.heightProperty());
        
        // Áp dụng mặt nạ vào khung nhìn
        mapViewPort.setClip(clipRect);
    }

    /**
     * Tạo các lớp trong suốt xếp chồng lên nhau
     */
    private void initLayers() {
        roadLayer = new Pane();
        trafficLightLayer = new Pane();
        carLayer = new Pane();
        truckLayer = new Pane();

        // Cho phép chuột bấm xuyên qua các lớp trên xuống dưới
        roadLayer.setPickOnBounds(false);
        trafficLightLayer.setPickOnBounds(false);
        carLayer.setPickOnBounds(false);
        truckLayer.setPickOnBounds(false);

        // Thêm vào StackPane (Cái nào add sau thì nằm đè lên trên)
        mapContainer.getChildren().addAll(roadLayer, trafficLightLayer, truckLayer, carLayer);
    }

    /**
     * Tính toán tỉ lệ Zoom (Bắt buộc chạy trước khi vẽ)
     */
    /**
     * Tính toán tỉ lệ Zoom (Đã sửa lỗi ClassCastException)
     */
    public void calculateScale() {
        try {
            // 1. Lấy biên giới hạn từ SUMO
            SumoGeometry boundaryGeo = (SumoGeometry) connection.do_job_get(Simulation.getNetBoundary());
            
            double x_min = Double.MAX_VALUE;
            double y_min = Double.MAX_VALUE;
            // Reset lại max
            this.x_max = -Double.MAX_VALUE;
            this.y_max = -Double.MAX_VALUE;

            for (SumoPosition2D point : boundaryGeo.coords) {
                if (point.x < x_min) x_min = point.x;
                if (point.y < y_min) y_min = point.y;
                if (point.x > this.x_max) this.x_max = point.x;
                if (point.y > this.y_max) this.y_max = point.y;
            }

            this.minX = x_min;
            this.minY = y_min;
            
            // Kích thước thực tế của bản đồ SUMO
            double sumoMapWidth = this.x_max - this.minX;
            double sumoMapHeight = this.y_max - this.minY;

            // Kích thước màn hình JavaFX
            double paneWidth = mapViewPort.getWidth();
            double paneHeight = mapViewPort.getHeight();
            
            if (paneWidth == 0) paneWidth = 1000;
            if (paneHeight == 0) paneHeight = 800;

            // 2. Tính Tỉ lệ (Scale)
            double scaleX = paneWidth / sumoMapWidth;
            double scaleY = paneHeight / sumoMapHeight;
            this.scale = Math.min(scaleX, scaleY) * 0.95; // Zoom 95% để chừa lề

            // 3. Tính Offset (Đẩy bản đồ ra giữa) - ĐÂY LÀ PHẦN QUAN TRỌNG MỚI THÊM
            double occupiedWidth = sumoMapWidth * this.scale;
            double occupiedHeight = sumoMapHeight * this.scale;

            this.xOffset = (paneWidth - occupiedWidth) / 2;
            this.yOffset = (paneHeight - occupiedHeight) / 2;

            System.out.println("Scale: " + scale + " | Offset X: " + xOffset + " | Offset Y: " + yOffset);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Chuyển đổi tọa độ X (Mét -> Pixel)
     */
    private double convertX(double sumoX) {
        // Công thức: Lề Trái + (Tọa độ thực - Tọa độ gốc) * Tỉ lệ
        return xOffset + ((sumoX - minX) * scale);
    }

    private double convertY(double sumoY) {
        // Công thức đảo trục Y + Lề Trên
        // (y_max - sumoY) là phép lật ngược trục Y chuẩn nhất
        return yOffset + ((y_max - sumoY) * scale);
    }

    /**
     * Vẽ đường xá (Chạy trên luồng riêng, vẽ vào roadLayer)
     */
    public void drawRoads() {
        Thread drawThread = new Thread(() -> {
            try {
                SumoStringList laneIdList = (SumoStringList) connection.do_job_get(Lane.getIDList());
                java.util.List<javafx.scene.Node> batch = new java.util.ArrayList<>();
                
                for (String laneId : laneIdList) {
                    // Bỏ qua các đoạn nối nội bộ (internal lanes) bắt đầu bằng dấu ":"
                    if (laneId.startsWith(":")) continue;

                    try {
                        SumoGeometry geometry = (SumoGeometry) connection.do_job_get(Lane.getShape(laneId));
                        Polyline laneLine = new Polyline();
                        laneLine.setStroke(Color.web("#ffffff"));
                        laneLine.setStrokeWidth(1.0);
                     // (Mẹo nâng cao) Tắt hiệu ứng làm mịn cạnh (Anti-aliasing) nếu muốn nét đanh và sắc cạnh hơn nữa (tùy chọn)
                        laneLine.setSmooth(false); 

                        for (SumoPosition2D point : geometry.coords) {
                            laneLine.getPoints().addAll(convertX(point.x), convertY(point.y));
                        }

                        
                        batch.add(laneLine);

                        // Gom 50 đường vẽ 1 lần cho mượt
                        if (batch.size() >= 50) {
                            final java.util.List<javafx.scene.Node> nodesToDraw = new java.util.ArrayList<>(batch);
                            Platform.runLater(() -> roadLayer.getChildren().addAll(nodesToDraw));
                            batch.clear();
                            Thread.sleep(5); 
                        }
                    } catch (Exception e) { continue; }
                }
                // Vẽ nốt phần dư
                if (!batch.isEmpty()) {
                    final java.util.List<javafx.scene.Node> remainingNodes = new java.util.ArrayList<>(batch);
                    Platform.runLater(() -> roadLayer.getChildren().addAll(remainingNodes));
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
        drawThread.setDaemon(true);
        drawThread.start();
    }

    /**
     * Cập nhật vị trí xe (Chạy liên tục)
     */
    public void updateVehicles() {
        try {
            SumoStringList activeVehicles = (SumoStringList) connection.do_job_get(Vehicle.getIDList());

            for (String vehicleId : activeVehicles) {
                SumoPosition2D pos = (SumoPosition2D) connection.do_job_get(Vehicle.getPosition(vehicleId));
                double angle = (double) connection.do_job_get(Vehicle.getAngle(vehicleId));
                SumoColor color = (SumoColor) connection.do_job_get(Vehicle.getColor(vehicleId));
                
                // Lấy loại xe để phân lớp (nếu có)
                // Nếu lỗi hàm getTypeID, tạm thời bỏ qua logic phân lớp
                String typeId = "car"; 
                try { typeId = (String) connection.do_job_get(Vehicle.getTypeID(vehicleId)); } catch (Exception e) {}

                if (vehicleMap.containsKey(vehicleId)) {
                    // --- CẬP NHẬT XE CŨ ---
                    Rectangle carShape = (Rectangle) vehicleMap.get(vehicleId);
                    
                    // Tâm xe = Tọa độ - (Chiều rộng / 2)
                    carShape.setX(convertX(pos.x) - carShape.getWidth() / 2);
                    carShape.setY(convertY(pos.y) - carShape.getHeight() / 2);
                    carShape.setRotate(angle); // Xoay xe theo hướng đường

                } else {
                    // --- TẠO XE MỚI ---
                    double width = 2.0 * scale;
                    double length = 5.0 * scale;
                    
                    // Giới hạn kích thước tối thiểu để nhìn thấy được
                    if (width < 3) width = 3;
                    if (length < 6) length = 6;

                    Rectangle carShape = new Rectangle(width, length);
                    carShape.setFill(Color.rgb(color.r, color.g, color.b));
                    carShape.setStroke(Color.BLACK);
                    carShape.setStrokeWidth(1);

                    carShape.setX(convertX(pos.x) - width / 2);
                    carShape.setY(convertY(pos.y) - length / 2);
                    carShape.setRotate(angle);

                    // Phân loại vào các lớp
                    if (typeId.toLowerCase().contains("truck") || typeId.toLowerCase().contains("bus")) {
                        truckLayer.getChildren().add(carShape);
                    } else {
                        carLayer.getChildren().add(carShape);
                    }

                    vehicleMap.put(vehicleId, carShape);
                }
            }

            // --- XÓA XE ĐÃ BIẾN MẤT ---
            ArrayList<String> toRemove = new ArrayList<>();
            for (String id : vehicleMap.keySet()) {
                if (!activeVehicles.contains(id)) {
                    toRemove.add(id);
                }
            }
            for (String id : toRemove) {
                Shape shape = vehicleMap.get(id);
                carLayer.getChildren().remove(shape);
                truckLayer.getChildren().remove(shape);
                vehicleMap.remove(id);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Các hàm bật tắt lớp (Visibility)
    public void setTrafficLightsVisible(boolean visible) { trafficLightLayer.setVisible(visible); }
    public void setCarsVisible(boolean visible) { carLayer.setVisible(visible); }
    public void setTrucksVisible(boolean visible) { truckLayer.setVisible(visible); }
    
    
    /**
     * Bật tính năng Zoom và Kéo thả bản đồ
     */
    private void enableZoomAndPan() {
        // 1. XỬ LÝ ZOOM (LĂN CHUỘT)
        mapContainer.setOnScroll(event -> {
            event.consume();

            double zoomFactor = 1.1; // Tốc độ zoom
            double deltaY = event.getDeltaY();

            if (deltaY < 0) {
                zoomFactor = 1 / zoomFactor; // Thu nhỏ
            }

            // Giới hạn zoom (Không cho quá nhỏ hoặc quá to)
            double currentScale = mapContainer.getScaleX();
            double newScale = currentScale * zoomFactor;
            
            if (newScale < 0.5) newScale = 0.5;
            if (newScale > 10.0) newScale = 10.0;

            // Áp dụng tỉ lệ mới
            mapContainer.setScaleX(newScale);
            mapContainer.setScaleY(newScale);
        });

        // 2. XỬ LÝ KÉO THẢ (PANNING)
        
        // Khi bấm chuột xuống: Ghi nhớ vị trí bắt đầu
        mapContainer.setOnMousePressed(event -> {
            mouseAnchorX = event.getSceneX();
            mouseAnchorY = event.getSceneY();
            translateAnchorX = mapContainer.getTranslateX();
            translateAnchorY = mapContainer.getTranslateY();
        });

        // Khi di chuột (đang giữ nút): Di chuyển bản đồ
        mapContainer.setOnMouseDragged(event -> {
            double dragX = event.getSceneX() - mouseAnchorX;
            double dragY = event.getSceneY() - mouseAnchorY;
            
            // Điều chỉnh vị trí mới = Vị trí cũ + Quãng đường kéo
            // Chia cho scale hiện tại để tốc độ kéo không bị quá nhanh khi zoom to
            double scale = mapContainer.getScaleX();
            mapContainer.setTranslateX(translateAnchorX + dragX);
            mapContainer.setTranslateY(translateAnchorY + dragY);
        });
    }
}