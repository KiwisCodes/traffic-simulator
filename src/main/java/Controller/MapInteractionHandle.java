package Controller;

import javafx.scene.layout.StackPane;

/**
 * Lớp xử lý tương tác người dùng với bản đồ (Zoom, Pan).
 * Tách biệt khỏi logic vẽ (Renderer) và logic nghiệp vụ (Controller).
 */
public class MapInteractionHandle {

    private final StackPane mapContainer;

    // Các biến lưu vị trí chuột để tính toán độ dịch chuyển (Panning)
    private double mouseAnchorX;
    private double mouseAnchorY;
    private double translateAnchorX;
    private double translateAnchorY;

    public MapInteractionHandle(StackPane mapContainer) {
        this.mapContainer = mapContainer;
        
        // Kích hoạt ngay khi khởi tạo
        enableZoomAndPan();
    }

    /**
     * Thiết lập các sự kiện chuột
     */
    private void enableZoomAndPan() {
        // 1. XỬ LÝ ZOOM (LĂN CHUỘT)
        mapContainer.setOnScroll(event -> {
            event.consume();

            // Hệ số zoom: >1 là phóng to, <1 là thu nhỏ
            double zoomFactor = 1.1; 
            double deltaY = event.getDeltaY();

            if (deltaY < 0) {
                zoomFactor = 1 / zoomFactor;
            }

            // Lấy tỉ lệ hiện tại
            double currentScale = mapContainer.getScaleX();
            double newScale = currentScale * zoomFactor;
            
            // Giới hạn zoom (Min 0.5x - Max 10x)
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
            // Tính quãng đường chuột đã di chuyển
            double dragX = event.getSceneX() - mouseAnchorX;
            double dragY = event.getSceneY() - mouseAnchorY;
            
            // Cập nhật vị trí mới cho bản đồ
            // Lưu ý: Không chia cho scale ở đây để cảm giác kéo thật tay hơn
            mapContainer.setTranslateX(translateAnchorX + dragX);
            mapContainer.setTranslateY(translateAnchorY + dragY);
        });
    }
}