package ProjectMain;

import gui.MainGUI; // MUST IMPORT MainGUI as it's in a different package

/**
 * ProjectMain: The main entry point for the entire application.
 * This class will eventually handle initialization, configuration, 
 * and orchestration between the GUI (MainGUI) and the simulation 
 * engine (MainSumo).
 */
public class ProjectMain {

    public static void main(String[] args) {
        // The core logic of ProjectMain will be here. 
        // For now, we launch the GUI.
        System.out.println("Starting Traffic Simulator Application...");

        // Launch the JavaFX GUI component
        // Note: MainGUI.main(args) handles the JavaFX environment setup.
        MainGUI.main(args);
        
        // In the future, you might start MainSumo in a separate thread here:
        // sumo.MainSumo.startEngine(args); // Requires another import/fully qualified name
    }
}