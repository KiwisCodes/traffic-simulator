package gui;

import de.tudresden.sumo.cmd.Lane;
import de.tudresden.sumo.cmd.Simulation;
import de.tudresden.sumo.objects.SumoBoundingBox;
import de.tudresden.sumo.objects.SumoGeometry;
import de.tudresden.sumo.objects.SumoPosition2D;
import de.tudresden.sumo.objects.SumoStringList;
import it.polito.appeal.traci.SumoTraciConnection;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polyline;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import javafx.scene.shape.Rectangle; // Dùng hình chữ nhật làm xe
import de.tudresden.sumo.cmd.Vehicle; // Lệnh điều khiển xe
import de.tudresden.sumo.objects.SumoColor;
import de.tudresden.sumo.objects.SumoPosition2D;
import de.tudresden.sumo.objects.SumoStringList;

public class MapRenderer {

    private final Pane mapPane;
    private final SumoTraciConnection connection;

    // Các biến tỉ lệ để vẽ bản đồ
    private double scale = 1.0;
    private double minX = 0;
    private double minY = 0;
    private double mapHeight = 0; // Chiều cao thực tế của vùng vẽ

    public MapRenderer(Pane mapPane, SumoTraciConnection connection) {
        this.mapPane = mapPane;
        this.connection = connection;
    }

    /**
     * BƯỚC 1: Tính toán tỉ lệ (Scale)
     * Lấy kích thước bản đồ từ SUMO và co giãn cho vừa với màn hình JavaFX
     */
    public void calculateScale() {
        try {
            // Gửi lệnh lấy khung bao quanh bản đồ (Boundary)
            SumoBoundingBox boundary = (SumoBoundingBox) connection.do_job_get(Simulation.getNetBoundary());

            this.minX = boundary.x_min;
            this.minY = boundary.y_min;
            double sumoWidth = boundary.x_max - boundary.x_min;
            double sumoHeight = boundary.y_max - boundary.y_min;

            // Lấy kích thước hiện tại của cái khung đen trên màn hình
            double paneWidth = mapPane.getWidth();
            double paneHeight = mapPane.getHeight();

            // Tính tỉ lệ zoom (giữ nguyên tỉ lệ khung hình để map không bị méo)
            double scaleX = paneWidth / sumoWidth;
            double scaleY = paneHeight / sumoHeight;
            
            // Nhân 0.95 để chừa lề một chút cho đẹp
            this.scale = Math.min(scaleX, scaleY) * 0.95;

            // Lưu chiều cao pane để dùng cho công thức đảo trục Y
            this.mapHeight = paneHeight;

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Không thể lấy kích thước bản đồ từ SUMO.");
        }
    }

    /**
     * BƯỚC 2: Vẽ các con đường (Static Map)
     * Hàm này chạy khá nặng, nên chạy 1 lần lúc khởi động.
     */
    public void drawRoads() {
        try {
            System.out.println("Đang tải dữ liệu bản đồ...");
            
            // 1. Lấy danh sách tất cả ID của các làn đường (Lanes)
            SumoStringList laneIdList = (SumoStringList) connection.do_job_get(Lane.getIDList());

            // 2. Duyệt qua từng làn đường để vẽ
            for (String laneId : laneIdList) {
                
                // Gửi lệnh lấy hình dáng (Shape) của làn đường
                SumoGeometry geometry = (SumoGeometry) connection.do_job_get(Lane.getShape(laneId));
                
                // Tạo đối tượng đường gấp khúc (Polyline) của JavaFX
                Polyline laneLine = new Polyline();
                
                // Màu sắc: Xám nhạt hoặc Trắng mờ để nổi trên nền đen
                laneLine.setStroke(Color.web("#666666")); 
                laneLine.setStrokeWidth(1.0); // Độ dày nét vẽ

                // Chuyển đổi từng điểm tọa độ từ SUMO -> JavaFX
                for (SumoPosition2D point : geometry.coords) {
                    double fxX = convertX(point.x);
                    double fxY = convertY(point.y);
                    laneLine.getPoints().addAll(fxX, fxY);
                }

                // Thêm đường vẽ vào màn hình
                mapPane.getChildren().add(laneLine);
            }
            
            System.out.println("Đã vẽ xong " + laneIdList.size() + " làn đường.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ========================================================
    // CÔNG THỨC TOÁN HỌC CHUYỂN ĐỔI TỌA ĐỘ (MÉT -> PIXEL)
    // ========================================================

    private double convertX(double sumoX) {
        // Công thức: (Giá trị thực - Giá trị nhỏ nhất) * Tỉ lệ
        return (sumoX - minX) * scale;
    }

    private double convertY(double sumoY) {
        // Công thức đảo trục Y: Chiều cao màn hình - (Giá trị Y đã scale)
        // Vì JavaFX tọa độ (0,0) nằm góc TRÊN cùng bên trái
        return mapHeight - ((sumoY - minY) * scale);
    }
    
    private Map<String, Rectangle> vehicleMap = new HashMap<>();
    /**
     * BƯỚC 3: Cập nhật vị trí xe (Chạy liên tục mỗi 0.1 giây)
     */
    public void updateVehicles() {
        try {
            // 1. Lấy danh sách ID các xe đang chạy từ SUMO
            SumoStringList activeVehicles = (SumoStringList) connection.do_job_get(Vehicle.getIDList());

            // 2. Duyệt qua từng xe đang chạy
            for (String vehicleId : activeVehicles) {

                // Lấy tọa độ (x, y) hiện tại của xe
                SumoPosition2D pos = (SumoPosition2D) connection.do_job_get(Vehicle.getPosition(vehicleId));

                // Lấy góc quay (angle) để đầu xe hướng đúng đường
                double angle = (double) connection.do_job_get(Vehicle.getAngle(vehicleId));

                // Lấy màu sắc xe
                SumoColor color = (SumoColor) connection.do_job_get(Vehicle.getColor(vehicleId));

                // --- LOGIC VẼ HOẶC CẬP NHẬT ---
                if (vehicleMap.containsKey(vehicleId)) {
                    // TRƯỜNG HỢP A: Xe đã có trên màn hình -> Chỉ cần di chuyển nó
                    Rectangle carShape = vehicleMap.get(vehicleId);

                    // Cập nhật vị trí mới (nhớ chuyển đổi sang Pixel)
                    carShape.setX(convertX(pos.x) - carShape.getWidth() / 2); // Trừ đi 1/2 chiều rộng để tâm xe nằm giữa đường
                    carShape.setY(convertY(pos.y) - carShape.getHeight() / 2);
                    carShape.setRotate(angle); // Xoay xe

                } else {
                    // TRƯỜNG HỢP B: Xe mới xuất hiện -> Tạo hình mới
                    // Xe con thường dài 5m, rộng 2m. Ta nhân với tỉ lệ scale để ra pixel
                    double carWidth = 2.0 * scale; 
                    double carLength = 5.0 * scale; 

                    // Đảm bảo xe không quá bé nếu zoom xa
                    if (carWidth < 2) carWidth = 2;
                    if (carLength < 4) carLength = 4;

                    Rectangle carShape = new Rectangle(carWidth, carLength);

                    // Đặt vị trí ban đầu
                    carShape.setX(convertX(pos.x) - carWidth / 2);
                    carShape.setY(convertY(pos.y) - carLength / 2);
                    carShape.setRotate(angle);

                    // Tô màu cho xe (Convert SumoColor -> JavaFX Color)
                    carShape.setFill(Color.rgb(color.r, color.g, color.b));
                    carShape.setStroke(Color.BLACK);
                    carShape.setStrokeWidth(1);

                    // Thêm vào màn hình và sổ điểm danh
                    mapPane.getChildren().add(carShape);
                    vehicleMap.put(vehicleId, carShape);
                }
            }

            // 3. Dọn dẹp những xe đã biến mất (Đã đến đích hoặc rẽ sang map khác)
            // Tìm những ID có trong sổ điểm danh (vehicleMap) NHƯNG không còn trong danh sách activeVehicles
            ArrayList<String> vehiclesToRemove = new ArrayList<>();
            for (String id : vehicleMap.keySet()) {
                if (!activeVehicles.contains(id)) {
                    vehiclesToRemove.add(id);
                }
            }

            // Xóa hình ảnh các xe đó khỏi màn hình
            for (String id : vehiclesToRemove) {
                Rectangle carShape = vehicleMap.get(id);
                mapPane.getChildren().remove(carShape); // Xóa khỏi giao diện
                vehicleMap.remove(id);                  // Xóa khỏi sổ quản lý
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}