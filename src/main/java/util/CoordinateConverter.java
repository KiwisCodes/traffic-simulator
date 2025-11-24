package util;

import de.tudresden.sumo.objects.SumoGeometry;
import de.tudresden.sumo.objects.SumoPosition2D;

/**
 * Lớp tiện ích chịu trách nhiệm chuyển đổi tọa độ từ SUMO (mét) sang JavaFX (pixel).
 * Tương ứng với yêu cầu "Separation of Concerns" trong Clean Code.
 */
public class CoordinateConverter {

    // Các biến lưu trạng thái tỉ lệ
    private double scale = 1.0;
    private double minX = 0;
    private double minY = 0;
    private double mapHeight = 0; // Chiều cao của khung nhìn để đảo trục Y
    private double xOffset = 0;
    private double yOffset = 0;

    // Biến tạm để tìm min/max
    private double x_max = 0;
    private double y_max = 0;

    /**
     * Tính toán tỉ lệ (Scale) và độ lệch (Offset) dựa trên biên giới hạn bản đồ và kích thước màn hình.
     * Logic này được tách từ hàm calculateScale() cũ.
     * * @param boundaryGeo Hình dáng biên giới hạn lấy từ SUMO
     * @param paneWidth Chiều rộng của khung nhìn (Viewport)
     * @param paneHeight Chiều cao của khung nhìn (Viewport)
     */
    public void calculateTransform(SumoGeometry boundaryGeo, double paneWidth, double paneHeight) {
        // 1. Tìm Min/Max từ dữ liệu SUMO
        double x_min = Double.MAX_VALUE;
        double y_min = Double.MAX_VALUE;
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

        // Phòng trường hợp pane chưa hiện lên (kích thước = 0)
        if (paneWidth == 0) paneWidth = 1000;
        if (paneHeight == 0) paneHeight = 800;

        // 2. Tính Tỉ lệ (Scale)
        double scaleX = paneWidth / sumoMapWidth;
        double scaleY = paneHeight / sumoMapHeight;
        this.scale = Math.min(scaleX, scaleY) * 0.95; // Zoom 95% để chừa lề

        // 3. Tính Offset (Đẩy bản đồ ra giữa)
        double occupiedWidth = sumoMapWidth * this.scale;
        double occupiedHeight = sumoMapHeight * this.scale;

        this.xOffset = (paneWidth - occupiedWidth) / 2;
        this.yOffset = (paneHeight - occupiedHeight) / 2;
        
        // Lưu chiều cao pane để dùng cho công thức đảo trục Y
        this.mapHeight = paneHeight;

        System.out.println("CoordinateConverter: Scale=" + scale + " | OffsetX=" + xOffset + " | OffsetY=" + yOffset);
    }

    /**
     * Chuyển đổi tọa độ X (Mét -> Pixel)
     */
    public double convertX(double sumoX) {
        return xOffset + ((sumoX - minX) * scale);
    }

    /**
     * Chuyển đổi tọa độ Y (Mét -> Pixel) - Đảo ngược trục Y
     */
    public double convertY(double sumoY) {
        // Công thức đảo trục Y chuẩn: OffsetY + ((Y_Max - sumoY) * scale)
        // Hoặc dùng công thức cũ của bạn nếu nó đang hoạt động tốt:
        return yOffset + ((y_max - sumoY) * scale);
    }

    // Getter cho scale nếu các class khác cần dùng (ví dụ để vẽ độ dày nét vẽ)
    public double getScale() {
        return scale;
    }
}