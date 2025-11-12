package gui;

// ❌ DELETE THIS: import java.awt.event.ActionEvent; 

// ✅ IMPORT THIS: The correct JavaFX Event class
import javafx.event.ActionEvent; 
import javafx.fxml.FXML;

public class Controller {
    
    // All methods that handle FXML events MUST have the @FXML annotation
    @FXML
    public void up(ActionEvent e) {
        System.out.println("up");
    }
    
    @FXML
    public void down(ActionEvent e) {
        System.out.println("down");
    }
    
    @FXML
    public void left(ActionEvent e) {
        System.out.println("left");
    }
    
    @FXML
    public void right(ActionEvent e) {
        System.out.println("right");
    }
}