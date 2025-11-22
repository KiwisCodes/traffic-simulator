package controller;

import javafx.scene.control.Label;

public class SimulationLogger {
    
    private static Label logLabel; // Reference to the GUI label

    public SimulationLogger() {}
    
    public static void setLogLabel(Label label) {
        logLabel = label;
    }

    public void log(String message) {
        System.out.println(message);
        if (logLabel != null) {
            // Prepend new message to top
            String oldText = logLabel.getText();
            logLabel.setText(message + "\n" + (oldText != null ? oldText : ""));
            logLabel = null;
        }
    }
}