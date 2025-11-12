package gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader; // ⬅️ REQUIRED for FXML
import javafx.scene.Parent;    // ⬅️ REQUIRED to hold the root FXML element
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * MainGUI: Handles the JavaFX user interface setup.
 * This class extends Application and sets up the primary window/stage by loading MainView.fxml.
 */
public class MainGUI extends Application {

    private static final String APP_TITLE = "Traffic Simulator GUI";

    /**
     * The start method now loads the MainView.fxml file.
     *
     * @param primaryStage the primary stage for this application.
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            // 1. Load the FXML file using the class's classpath.
            // This tells Java to look for MainView.fxml in the same package 
            // folder structure under src/main/resources (i.e., src/main/resources/gui/MainView.fxml).
            FXMLLoader loader = new FXMLLoader(getClass().getResource("MainView.fxml"));
            
            // 2. Load the root element defined in the FXML file.
            Parent root = loader.load();
            
            // 3. Create the scene using the FXML content.
            Scene scene = new Scene(root);
            
            // 4. Set the stage properties
            primaryStage.setTitle(APP_TITLE);
            primaryStage.setScene(scene);
            
            // 5. Show the window
            primaryStage.show();
            
        } catch (IOException e) {
            // This catch block runs if the FXML file cannot be found or has errors.
            System.err.println("❌ CRITICAL ERROR: Could not load MainView.fxml.");
            System.err.println("Ensure MainView.fxml is in src/main/resources/gui/ and is valid FXML.");
            e.printStackTrace();
        }
    }
    
    /**
     * Standard main method to launch the JavaFX application.
     */
    public static void main(String[] args) {
        launch(args);
    }
}