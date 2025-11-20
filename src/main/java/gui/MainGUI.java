package gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class MainGUI extends Application {

    // Reference the new main view file
    private static final String FXML_VIEW = "/gui/MainView.fxml"; 

    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("Starting Traffic Simulator Application...");
        
        // Attempt to get the resource URL
        URL fxmlUrl = getClass().getResource(FXML_VIEW);
        
        if (fxmlUrl == null) {
            // Robust error message if the file is missing from the classpath
            System.err.println("--- FATAL ERROR ---");
            System.err.println("FXML resource not found: " + FXML_VIEW);
            System.err.println("Please ensure the file is located at src/main/resources" + FXML_VIEW);
            System.err.println("-------------------");
            throw new IllegalStateException("FXML resource not found at " + FXML_VIEW);
        }
        
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent root = loader.load();

        primaryStage.setTitle("SUMO Traffic Simulator");
        primaryStage.setScene(new Scene(root, 1400, 900)); // Set default size
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}