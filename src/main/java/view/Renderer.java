package view;

import de.tudresden.sumo.cmd.Lane;
import de.tudresden.sumo.cmd.Vehicle;
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
import util.CoordinateConverter; // Import class toán học mới

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Renderer {

    private final Pane mapViewPort;       // Khung nhìn (Cố định)
    private final StackPane mapContainer; // Nội dung bản đồ
    private final SumoTraciConnection connection;
    private final CoordinateConverter converter; // CÔNG CỤ TOÁN HỌC (Mới)

    // CÁC LỚP (LAYERS)
    private Pane roadLayer;
    private Pane trafficLightLayer;
    private Pane carLayer;
    private Pane truckLayer;

    // Quản lý hình ảnh xe
    private Map<String, Shape> vehicleMap = new HashMap<>();

    /**
     * Constructor mới: Nhận thêm CoordinateConverter
     */
    public Renderer(Pane mapViewPort, StackPane mapContainer, SumoTraciConnection connection, CoordinateConverter converter) {
        this.mapViewPort = mapViewPort;
        this.mapContainer = mapContainer;
        this.connection = connection;
        this.converter = converter; // Lưu công cụ toán học để dùng

        // Cài đặt màu nền
        this.mapViewPort.setStyle("-fx-background-color: #2b2b2b; -fx-border-width: 0;");
        this.mapContainer.setStyle("-fx-background-color: transparent; -fx-border-width: 0; -fx-border-color: transparent;");

        initLayers();
        setupClipping();
        
        // Lưu ý: Logic Zoom/Pan đã bị xóa khỏi đây để chuyển sang MapInteractionHandler
    }

    private void initLayers() {
        roadLayer = new Pane();
        trafficLightLayer = new Pane();
        carLayer = new Pane();
        truckLayer = new Pane();

        roadLayer.setPickOnBounds(false);
        trafficLightLayer.setPickOnBounds(false);
        carLayer.setPickOnBounds(false);
        truckLayer.setPickOnBounds(false);

        mapContainer.getChildren().addAll(roadLayer, trafficLightLayer, truckLayer, carLayer);
    }

    private void setupClipping() {
        Rectangle clipRect = new Rectangle();
        clipRect.widthProperty().bind(mapViewPort.widthProperty());
        clipRect.heightProperty().bind(mapViewPort.heightProperty());
        mapViewPort.setClip(clipRect);
    }

    /**
     * Vẽ đường xá (Sử dụng converter để tính tọa độ)
     */
    public void drawRoads() {
        Thread drawThread = new Thread(() -> {
            try {
                System.out.println("Renderer: Bắt đầu vẽ đường...");
                SumoStringList laneIdList = (SumoStringList) connection.do_job_get(Lane.getIDList());
                java.util.List<javafx.scene.Node> batch = new java.util.ArrayList<>();

                for (String laneId : laneIdList) {
                    if (laneId.startsWith(":")) continue;

                    try {
                        SumoGeometry geometry = (SumoGeometry) connection.do_job_get(Lane.getShape(laneId));
                        Polyline laneLine = new Polyline();
                        laneLine.setStroke(Color.web("#ffffff"));
                        laneLine.setStrokeWidth(1.0);
                        laneLine.setSmooth(false);

                        for (SumoPosition2D point : geometry.coords) {
                            // --- GỌI CONVERTER THAY VÌ TỰ TÍNH ---
                            double fxX = converter.convertX(point.x);
                            double fxY = converter.convertY(point.y);
                            laneLine.getPoints().addAll(fxX, fxY);
                        }

                        batch.add(laneLine);

                        if (batch.size() >= 50) {
                            final java.util.List<javafx.scene.Node> nodesToDraw = new java.util.ArrayList<>(batch);
                            Platform.runLater(() -> roadLayer.getChildren().addAll(nodesToDraw));
                            batch.clear();
                            Thread.sleep(5);
                        }
                    } catch (Exception e) { continue; }
                }
                
                if (!batch.isEmpty()) {
                    final java.util.List<javafx.scene.Node> remainingNodes = new java.util.ArrayList<>(batch);
                    Platform.runLater(() -> roadLayer.getChildren().addAll(remainingNodes));
                }
                System.out.println("Renderer: Vẽ đường hoàn tất.");
            } catch (Exception e) { e.printStackTrace(); }
        });
        drawThread.setDaemon(true);
        drawThread.start();
    }

    /**
     * Cập nhật xe (Sử dụng converter)
     */
    public void updateVehicles() {
        try {
            SumoStringList activeVehicles = (SumoStringList) connection.do_job_get(Vehicle.getIDList());

            for (String vehicleId : activeVehicles) {
                SumoPosition2D pos = (SumoPosition2D) connection.do_job_get(Vehicle.getPosition(vehicleId));
                double angle = (double) connection.do_job_get(Vehicle.getAngle(vehicleId));
                SumoColor color = (SumoColor) connection.do_job_get(Vehicle.getColor(vehicleId));
                
                String typeId = "car";
                try { typeId = (String) connection.do_job_get(Vehicle.getTypeID(vehicleId)); } catch (Exception e) {}

                if (vehicleMap.containsKey(vehicleId)) {
                    Rectangle carShape = (Rectangle) vehicleMap.get(vehicleId);
                    
                    // --- GỌI CONVERTER ---
                    carShape.setX(converter.convertX(pos.x) - carShape.getWidth() / 2);
                    carShape.setY(converter.convertY(pos.y) - carShape.getHeight() / 2);
                    carShape.setRotate(angle);

                } else {
                    // Lấy tỉ lệ scale từ converter để tính kích thước xe
                    double currentScale = converter.getScale();
                    
                    double width = 2.0 * currentScale;
                    double length = 5.0 * currentScale;

                    if (width < 3) width = 3;
                    if (length < 6) length = 6;

                    Rectangle carShape = new Rectangle(width, length);
                    carShape.setFill(Color.rgb(color.r, color.g, color.b));
                    carShape.setStroke(Color.BLACK);
                    carShape.setStrokeWidth(1);

                    // --- GỌI CONVERTER ---
                    carShape.setX(converter.convertX(pos.x) - width / 2);
                    carShape.setY(converter.convertY(pos.y) - length / 2);
                    carShape.setRotate(angle);

                    if (typeId.toLowerCase().contains("truck") || typeId.toLowerCase().contains("bus")) {
                        truckLayer.getChildren().add(carShape);
                    } else {
                        carLayer.getChildren().add(carShape);
                    }

                    vehicleMap.put(vehicleId, carShape);
                }
            }

            ArrayList<String> toRemove = new ArrayList<>();
            for (String id : vehicleMap.keySet()) {
                if (!activeVehicles.contains(id)) toRemove.add(id);
            }
            for (String id : toRemove) {
                Shape shape = vehicleMap.get(id);
                carLayer.getChildren().remove(shape);
                truckLayer.getChildren().remove(shape);
                vehicleMap.remove(id);
            }

        } catch (Exception e) { e.printStackTrace(); }
    }
    
    // Getter/Setter Visibility...
    public void setCarsVisible(boolean visible) { carLayer.setVisible(visible); }
    // ...
}