package model;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.polito.appeal.traci.*;
import de.tudresden.sumo.cmd.*;
// Import your vehicle classes
import model.vehicles.VehicleManager;
//import model.vehicles.Car;
//import model.vehicles.Bus;
//import model.vehicles.Truck;
//import model.vehicles.Bike;

// Import Infrastructure
import model.infrastructure.MapManger;
import model.infrastructure.TrafficlightManager;

/**
 * The Core Logic Engine of the Application.
 * Responsible for:
 * 1. Maintaining the TraCI connection.
 * 2. Running the simulation loop in a background thread.
 * 3. Keeping the "State of the World" (Vehicles, Traffic Lights) up to date.
 * 4. Providing Thread-Safe data snapshots to the GUI Controller.
 */
public class SimulationManager {

    // --- Configuration ---
    // Adjust this path to match your system
    private String sumoPath = "/Users/apple/sumo/bin/sumo"; 
    private String sumoConfigFileName = "frauasmap.sumocfg";
    private String sumoConfigFilePath;
    
    // Step length in seconds (0.001s is very granular/fast)
    private String stepLength = "0.001"; 

    // --- TraCI Connection ---
    private SumoTraciConnection sumoConnection;

    // --- THREAD SYNCHRONIZATION LOCK ---
    // This object acts as a "key". Only one thread can hold this key at a time.
    // We use it to prevent the GUI from reading the vehicle list while the 
    // Simulation thread is deleting/adding to it.
    private final Object stateLock = new Object();

    // --- State Data (The "World") ---
    // 'volatile' ensures that changes to the reference are immediately visible to other threads
    private volatile List<VehicleManager> activeVehicles;
    private volatile Map<String, TrafficlightManager> activeTrafficlights;
    
    // Sub-Managers & Infrastructure
    private StatisticsManager statisticsManager;
    private ReportManager reportManager;
    private MapManger sumoMap; // Holds static map data (Lanes, Edges)
    private VehicleManager vehicleManager;
    private TrafficlightManager trafficlightManager;
    
    private volatile int currentStep = 0;
    private volatile boolean isRunning = false;

    // --- Constructor ---
    public SimulationManager() {
        this.activeVehicles = new ArrayList<>();
        this.activeTrafficlights = new HashMap<>();
        this.statisticsManager = new StatisticsManager();
        this.reportManager = new ReportManager();
        // sumoMap will be initialized after connection
    }

    // ====================================================================
    // 1. CONNECTION SETUP
    // ====================================================================

    /**
     * Connects to the SUMO server.
     * This is a BLOCKING call (takes time). Should be run on a background thread (Task).
     */
    public boolean startConnection() {
        if (!setupPaths()) return false;

        System.out.println("Creating connection with:");
        System.out.println("  > Binary: " + this.sumoPath);
        System.out.println("  > Config: " + this.sumoConfigFilePath);
        
        this.sumoConnection = new SumoTraciConnection(this.sumoPath, this.sumoConfigFilePath);
        this.sumoConnection.addOption("start", null); // Auto-start simulation
        this.sumoConnection.addOption("step-length", this.stepLength);
        this.sumoConnection.printSumoOutput(true);
        this.sumoConnection.printSumoError(true);

        try {
            System.out.println("⏳ Launching SUMO... (This may pause until TraCI connects)");
            this.sumoConnection.runServer(); // Starts the SUMO process
            
            // Load Static Map Data (Edges/Bounds) immediately after connecting
            this.sumoMap = new MapManger(sumoConnection);
            // You would call methods here to populate sumoMap using TraCI calls:
            // loadStaticMapData(); (Implementation logic from previous chat)
            
            System.out.println("✅ Connection established!");
            this.isRunning = true;
            return true;

        } catch (Exception e) {
            System.err.println("❌ Error starting SUMO: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Resolves file paths for config.
     */
    private boolean setupPaths() {
        try {
            URL resource = SimulationManager.class.getClassLoader().getResource(this.sumoConfigFileName);
            if (resource == null) {
                System.err.println("❌ CRITICAL: '" + this.sumoConfigFileName + "' not found in resources!");
                return false;
            }
            File file = new File(resource.toURI());
            this.sumoConfigFilePath = file.getAbsolutePath();
            
            File sumoBin = new File(this.sumoPath);
            if(!sumoBin.exists() || !sumoBin.canExecute()) {
                System.err.println("❌ CRITICAL: SUMO binary not found at: " + this.sumoPath);
                return false;
            }
            return true;
        } catch (Exception e) {
            System.err.println("❌ Error resolving file paths: " + e.getMessage());
            return false;
        }
    }

    // ====================================================================
    // 2. SIMULATION LOOP
    // ====================================================================

    /**
     * Main loop running on Thread 2 (Simulation Thread).
     */
    public void runSimulationLoop() {
        System.out.println("   -> Simulation Loop Started.");

        while (isRunning && !this.sumoConnection.isClosed()) {
            step();
            
            // Optional: Throttle speed to avoid 100% CPU usage if stepLength is tiny
            try { Thread.sleep(10); } catch (InterruptedException e) { break; }
        }
        
        stopSimulation();
        System.out.println("✅ Simulation loop finished.");
    }

    /**
     * Executes ONE single simulation step.
     * This method is Thread-Safe using the 'Snapshot' pattern.
     */
    public void step() {
        try {
            // --- PHASE 1: Heavy Lifting (Network I/O) ---
            // We do this OUTSIDE the lock so the GUI doesn't freeze waiting for TraCI.
            this.sumoConnection.do_timestep();
            
            // Fetch new vehicle data into a TEMPORARY local list (not implemented yet)
//            List<SumoVehicle> nextStepVehicles = fetchVehicleData(); 

            // --- PHASE 2: Safe Swap (Memory Operation) ---
            // We lock only for the split-second it takes to swap the reference.
            synchronized (stateLock) {
//                this.activeVehicles = nextStepVehicles; // Atomic reference swap
                this.currentStep++;
                
//                 Update stats safely while we hold the lock
                if (statisticsManager != null) {
                    statisticsManager.updateStatistics(0, activeVehicles.size());
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error during timestep " + currentStep);
            e.printStackTrace();
            stopSimulation(); 
        }
    }

    /**
     * Helper: Talks to TraCI to get the list of current vehicles.
     * @return A fresh list of vehicle objects.
     */
//    private List<SumoVehicle> fetchVehicleData() throws Exception {
//        List<SumoVehicle> newList = new ArrayList<>();
//        
//        // 1. Get IDs of all vehicles in the simulation right now
//        List<String> currentIds = (List<String>) sumoConnection.do_job_get(Vehicle.getIDList());
//        
//        for (String id : currentIds) {
//            // For simplification, we recreate objects to ensure fresh data.
//            // In a production app, you would cache these and only update position.
//            
//            // Determine Type (Car/Bus/etc) - Mocking logic here for simplicity
//            // String type = (String) sumoConnection.do_job_get(Vehicle.getTypeID(id));
//            // For now, assume Car:
//            SumoVehicle v = new Car(id); 
//            
//            // Get Position
//            // de.tudresden.sumo.objects.SumoPosition2D pos = 
//            //    (de.tudresden.sumo.objects.SumoPosition2D) sumoConnection.do_job_get(Vehicle.getPosition(id));
//            
//            // v.setPosition(pos);
//            
//            newList.add(v);
//        }
//        return newList;
//    }

    public void stopSimulation() {
        this.isRunning = false;
        if (this.sumoConnection != null && !this.sumoConnection.isClosed()) {
            this.sumoConnection.close();
            System.out.println("🔌 Connection closed.");
        }
    }

    // ====================================================================
    // 3. THREAD-SAFE GETTERS (For the GUI Controller)
    // ====================================================================

    /**
     * Returns a Safe SNAPSHOT of the active vehicles.
     * The GUI can iterate over this list without crashing, even if the
     * simulation thread updates the "real" list in the background.
     */
    public List<VehicleManager> getActiveVehicles() {
        synchronized (stateLock) {
/*
Synchornize keyword
synchronized (The Lock)

What it does: It creates a mutex (mutual exclusion). Only one thread can execute a block of code protected by the same lock at a time.

Analogy: A bathroom with a key. If Thread A is inside, Thread B must wait outside until A leaves.

Use case: When you need to perform multiple operations that must happen together (atomic), 
like clearing a list and adding new items. 
If you don't lock, another thread might see the list empty in the middle of your update.
 */
            if (activeVehicles == null) {
                return new ArrayList<>();
            }
            // Return a COPY (Snapshot)
            return new ArrayList<>(activeVehicles);
        }
        /*

1. The Relationship between List and ArrayList

List (The Interface): Think of List as a contract or a menu. 
It defines what you can do (add, remove, get item at index),
but it doesn't say how the data is stored in memory. It's an abstract concept.

ArrayList (The Implementation): This is a specific class that fulfills the List contract. 
It stores data in a resizing array. It says, "I am a List, and I work by using an array internally."

The Rule: Since ArrayList implements List, an ArrayList IS-A List. 
Therefore, any method that promises to return a List can legally return an ArrayList, a LinkedList, or any other list type.
         */
    }

    
//Change the below for traffic lights
//    public List<SumoVehicle> getActiveVehicles() {
//        synchronized (stateLock) {
//            if (activeVehicles == null) {
//                return new ArrayList<>();
//            }
//            // Return a COPY (Snapshot)
//            return new ArrayList<>(activeVehicles);
//        }
//    }
    
    
    

    public StatisticsManager getStatisticsManager() { return statisticsManager; }
    public ReportManager getReportManager() { return reportManager; }
    public int getCurrentStep() { return currentStep; } // Volatile makes this safe
    public SumoTraciConnection getConnection() { return sumoConnection; }
    public MapManger getSumoMap() { return sumoMap; }
}