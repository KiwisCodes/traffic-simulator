package gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainGUI extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // 1. Tìm và nạp file FXML (Bản vẽ giao diện)
            // Lưu ý: Đường dẫn này phải khớp với vị trí file trong src/main/resources/gui
        	FXMLLoader loader = new FXMLLoader(getClass().getResource("MainView.fxml"));
            Parent root = loader.load();

            // 2. Tạo Scene (Khung cảnh bên trong cửa sổ)
            Scene scene = new Scene(root);
            
            // (Tùy chọn) Nếu file css chưa ăn, có thể thêm thủ công ở đây:
            // scene.getStylesheets().add(getClass().getResource("/gui/style.css").toExternalForm());

            // 3. Cấu hình Cửa sổ chính (Stage)
            primaryStage.setTitle("Traffic Simulator - Winter 2025"); // Tiêu đề cửa sổ
            primaryStage.setScene(scene);
            
            // Đặt kích thước tối thiểu để không bị vỡ giao diện khi thu nhỏ
            primaryStage.setMinWidth(1000); 
            primaryStage.setMinHeight(700);

            // 4. Hiển thị lên màn hình
            primaryStage.show();
            
        } catch(Exception e) {
            e.printStackTrace();
            System.err.println("Lỗi: Không thể khởi chạy giao diện. Hãy kiểm tra đường dẫn file FXML.");
        }
    }

    public static void main(String[] args) {
        // Lệnh này kích hoạt phương thức start() ở trên
        launch(args);
    }
}