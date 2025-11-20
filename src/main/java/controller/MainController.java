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
    @FXML private TextField pathToSumoGui;
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

    // --- SUMO & TraaS ---
    private String sumoConfigPath;

    // --- Visualization Data ---
    private double minX = Double.MAX_VALUE;
    private double minY = Double.MAX_VALUE;
    private double maxX = -Double.MAX_VALUE;
    private double maxY = -Double.MAX_VALUE;

    // SCALING FACTORS
    private double baseScaleFactor = 1.0;
    private double currentZoom = 1.0;
    private final double PADDING = 50.0;

    // NEW: Group to hold content. Group auto-sizes to fit children!
    private Group mapContentGroup;

    @FXML
    public void initialize() {
        log("Initialization started.");
        // 1. Setup the Container Structure for proper Scrolling
        // We wrap the Panes in a Group. The ScrollPane will scroll this Group.
        mapContentGroup = new Group();
        // Add all your layers to this group
        mapContentGroup.getChildren().addAll(routePane, carPane, busPane, truckPane, bikePane);
        // Add the Group to the StackPane (which centers it)
        rootStackPane.getChildren().clear(); // Clear placeholder
        rootStackPane.getChildren().add(mapContentGroup);

        try {
            URL netUrl = getClass().getResource("/frauasmap.net.xml");
            if (netUrl == null) {
                log("FATAL ERROR: frauasmap.net.xml URL not found.");
                return;
            }
            drawMapNetwork(netUrl);

        } catch (Exception e) {
            log("FATAL ERROR initializing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void drawMapNetwork(URL netXmlUrl) {
        log("--- Starting Map Drawing Process ---");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(netXmlUrl.toExternalForm());
            doc.getDocumentElement().normalize();

            NodeList laneNodes = doc.getElementsByTagName("lane");
            int laneCount = laneNodes.getLength();

            // --- Step 1: Parse ALL lanes first to find TRUE BOUNDS ---
            List<List<Double>> allLanePoints = new ArrayList<>();

            for (int i = 0; i < laneCount; i++) {
                Element lane = (Element) laneNodes.item(i);
                String shapeStr = lane.getAttribute("shape");
                if (shapeStr.isEmpty()) continue;
                List<Double> currentLanePoints = new ArrayList<>();
                String[] points = shapeStr.split(" ");
                for (String point : points) {
                    String[] coords = point.split(",");
                    if (coords.length == 2) {
                        try {
                            double x = Double.parseDouble(coords[0]);
                            double y = Double.parseDouble(coords[1]);
                            currentLanePoints.add(x);
                            currentLanePoints.add(y);
                            if (x < minX) minX = x;
                            if (x > maxX) maxX = x;
                            if (y < minY) minY = y;
                            if (y > maxY) maxY = y;
                        } catch (NumberFormatException e) { }
                    }
                }
                allLanePoints.add(currentLanePoints);
            }

            // --- Step 2: Calculate Base Scale Factor ---
            double mapWidth = maxX - minX;
            double mapHeight = maxY - minY;
            if (mapWidth <= 0) mapWidth = 1000;
            if (mapHeight <= 0) mapHeight = 1000;

            double availableWidth = mapScrollPane.getPrefWidth() - (PADDING * 2);
            double availableHeight = mapScrollPane.getPrefHeight() - (PADDING * 2);
            if (availableWidth <= 0) availableWidth = 1108 - PADDING;
            if (availableHeight <= 0) availableHeight = 746 - PADDING;

            double scaleX = availableWidth / mapWidth;
            double scaleY = availableHeight / mapHeight;
            // Fit to screen
            baseScaleFactor = Math.min(scaleX, scaleY);

            // * FORCE SCROLLING *
            // We multiply by 1.2 (120% zoom) so the map is slightly larger than the screen.
            // This forces scrollbars to appear.
            currentZoom = 15;

            log(String.format("Map Bounds: (%.2f, %.2f) to (%.2f, %.2f)", minX, minY, maxX, maxY));
            log(String.format("Base Scale: %.4f | Current Zoom: %.2f", baseScaleFactor, currentZoom));

            // --- Step 3: Resize the Panes ---
            // This is CRITICAL. We must tell the panes exactly how big they are.
            double displayWidth = mapWidth * baseScaleFactor * currentZoom + (PADDING * 2);
            double displayHeight = mapHeight * baseScaleFactor * currentZoom + (PADDING * 2);
            setPaneSizes(routePane, displayWidth, displayHeight);
            setPaneSizes(carPane, displayWidth, displayHeight);
            setPaneSizes(busPane, displayWidth, displayHeight);
            setPaneSizes(truckPane, displayWidth, displayHeight);
            setPaneSizes(bikePane, displayWidth, displayHeight);

            // --- Step 4: Draw the shapes ---
            for (List<Double> points : allLanePoints) {
                Polyline roadShape = new Polyline();
                for (int k = 0; k < points.size(); k += 2) {
                    double x = points.get(k);
                    double y = points.get(k+1);
                    roadShape.getPoints().add(transformX(x));
                    roadShape.getPoints().add(transformY(y));
                }

                roadShape.setStroke(Color.GRAY);
                roadShape.setStrokeWidth(1);
                routePane.getChildren().add(roadShape);
            }
            log("SUCCESS: Map drawing complete.");

        } catch (Exception e) {
            e.printStackTrace();
            log("Error drawing map: " + e.getMessage());
        }
    }

    private void setPaneSizes(Pane pane, double w, double h) {
        pane.setPrefSize(w, h);
        pane.setMinSize(w, h);
        pane.setMaxSize(w, h);
    }

    // --- Coordinate Transformation Helpers ---

    private double transformX(double sumoX) {
        // (X - Min) * Scale * Zoom + Padding
        return ((sumoX - minX) * baseScaleFactor * currentZoom) + PADDING;
    }

    private double transformY(double sumoY) {
        // (Max - Y) * Scale * Zoom + Padding
        return ((maxY - sumoY) * baseScaleFactor * currentZoom) + PADDING;
    }

    private void log(String message) {
        System.out.println(message);
        if (logLabel != null) {
            logLabel.setText(message + "\n" + logLabel.getText());
        }
    }

    // --- Placeholder Action Methods ---
    @FXML private void startSimulation() {}
    @FXML private void pauseSimulation() {}
    @FXML private void stepSimulation() {}
    @FXML private void injectVehicle() {}
    @FXML private void startSumoGUI() {}
    @FXML private void insertSumoConfigFile() {}
}