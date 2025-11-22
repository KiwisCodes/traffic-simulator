package controller;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import model.SimulationManager;
import javafx.scene.layout.Pane;

// Import the correct TraaS library (DLR version)
import de.tudresden.sumo.cmd.*;
import de.tudresden.ws.container.SumoPosition2D;
import it.polito.appeal.traci.SumoTraciConnection;

// Java XML Parsing Imports
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

// Java Imports
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainController {

    // --- FXML View Elements ---
    @FXML private ScrollPane leftControlPanel;
    @FXML private ScrollPane mapScrollPane;
    @FXML private StackPane rootStackPane;

    // Simulation Control Done
    @FXML private Button startButton;
    @FXML private Button pauseButton;
    @FXML private Button stepButton;

    // Vehicle Actions Done
    @FXML private TextField vehicleIdField;
    @FXML private TextField routeIdField;
    @FXML private Button injectVehicleButton;
    @FXML private Button setVehicleSpeedButton;
    @FXML private TextField vehicleSpeedField;
    @FXML private Button setVehicleColorButton;
    @FXML private TextField vehicleColorField;

    // Traffic Light Actions Done
    @FXML private TextField trafficLightIdField;
    @FXML private Button setRedPhaseButton;
    @FXML private Button setGreenPhaseButton;
    @FXML private Button resumeAutoButton;
    @FXML private Button setPhaseDurationButton;
    @FXML private TextField phaseDurationField;
    @FXML private CheckBox adaptiveTrafficCheck;

    // Vehicle Filtering Done
    @FXML private TextField filterColorField;
    @FXML private TextField filterMinSpeedField;
    @FXML private TextField filterEdgeField;
    @FXML private Button applyFilterButton;
    @FXML private Button clearFilterButton;

    // Stress Testing Done
    @FXML private TextField stressEdgeField;
    @FXML private TextField stressCountField;
    @FXML private Button stressTestButton;

    // Sumo-GUI Integration
    @FXML private TextField pathToSumocfgFile;
    @FXML private TextField pathToSumoGui; //or Sumo
    @FXML private Button insertSumocfgButton;
    @FXML private Button startSumoGuiButton;

    // Live Statistics
    @FXML private Label simStepLabel;
    @FXML private Label vehicleCountLabel;
    @FXML private Label avgSpeedLabel;
    @FXML private Label avgTravelTimeLabel;
    @FXML private Label congestionLabel;
    @FXML private Button showChartsButton;

    // Data Export
    @FXML private CheckBox exportFilterCheck;
    @FXML private Button exportCsvButton;
    @FXML private Button exportPdfButton;

    // Map & Log
    @FXML private Pane routePane;
    @FXML private Pane carPane;
    @FXML private Pane busPane;
    @FXML private Pane truckPane;
    @FXML private Pane bikePane;
    @FXML private Label logLabel;
    @FXML private Button zoomInButton;
    @FXML private Button zoomOutButton;
    @FXML private Button resetViewButton;
    @FXML private ToggleButton toggle3DButton;

 // --- Logic & State ---
    private SimulationManager simManager;
    private Thread simulationThread; // Thread 2: The Engine
    private AnimationTimer uiLoop;   // Thread 1: The UI Renderer
    

    // Map to track visual shapes: ID -> Shape
    private Map<String, Shape> vehicleVisuals = new HashMap<>();

    // --- Visualization Data ---
    private double minX = Double.MAX_VALUE;
    private double minY = Double.MAX_VALUE;
    private double maxX = -Double.MAX_VALUE;
    private double maxY = -Double.MAX_VALUE;

    // SCALING FACTORS
    private double baseScaleFactor = 1.0;
    private double currentZoom = 1.0;
    private final double PADDING = 50.0;
    
    MainController(){
    	this.simManager = new SimulationManager();
    	this.simulationThread = new Thread();
    	
    }
    
    public static void main (String[] args) {
    	//This main function is the temporary app entry, run as java applicatin here
    	MainController newController = new MainController();
    	newController.startSimulation();
    	
    	
    }
   

    @FXML
    public void initialize() {
        
    }

    // --- Coordinate Transformation Helpers ---




    // --- Placeholder Action Methods ---
    @FXML private void startSimulation() {
    	this.simManager.startConnection();
    }
    
    
    @FXML private void pauseSimulation() {}
    @FXML private void stepSimulation() {}
    @FXML private void injectVehicle() {}
    @FXML private void startSumoGUI() {}
    @FXML private void insertSumoConfigFile() {}
}