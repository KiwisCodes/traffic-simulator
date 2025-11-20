package gui; // <- 1. SỬA LỖI PACKAGE: Đảm bảo dòng này là dòng đầu tiên

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color; // Thêm import này

public class Controller {

	@FXML
	public void handleConnectButton() {
	    System.out.println("Connect button clicked!");
	    // Code kết nối SUMO sẽ viết vào đây sau
	}

}