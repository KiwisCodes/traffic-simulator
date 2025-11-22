package model;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.polito.appeal.traci.*;
import de.tudresden.sumo.cmd.*;
// Assuming these classes exist in your project structure
import model.vehicles.SumoVehicle;
import model.infrastructure.SumoTrafficlight;

public class SimulationManager {

    // --- Configuration ---
    // Make sure this path is correct and points to the executable, not the folder
    private String sumoPath = "/Users/apple/sumo/bin/sumo"; 
    private String sumoConfigFileName = "frauasmap.sumocfg";
    private String sumoConfigFilePath;
    private String stepLength = "0.001"; // Standard step length is usually 0.1s or 1.0s

    // --- TraCI Connection ---
    private SumoTraciConnection sumoConnection;

    // --- State Data ---
    private List<SumoVehicle> activeVehicles;
    private Map<String, SumoTrafficlight> activeTrafficlights;
    private StatisticsManager statisticsManager;
    private ReportManager reportManager;
    private int currentStep = 0;

    public SimulationManager() {
        this.activeVehicles = new ArrayList<>();
        this.activeTrafficlights = new HashMap<>();
        this.statisticsManager = new StatisticsManager();
        this.reportManager = new ReportManager();
    }

    /**
     * Main entry point to start the simulation.
     * Returns true if simulation started and finished loop successfully.
     */
    public void startConnection() {
        // 1. RESOLVE FILE PATHS
        if (!setupPaths()) {
            return;
        }

        // 2. INITIALIZE CONNECTION OBJECT
        // We print the paths to console to verify they are correct before running
        System.out.println("Creating connection with:");
        System.out.println("  > Binary: " + this.sumoPath);
        System.out.println("  > Config: " + this.sumoConfigFilePath);
        
        this.sumoConnection = new SumoTraciConnection(this.sumoPath, this.sumoConfigFilePath);

        // 3. CONFIGURE SUMO OPTIONS
        // "start" -> null means simple flag "--start" (starts sim immediately without waiting for 'play' button)
        this.sumoConnection.addOption("start", null); 
        this.sumoConnection.addOption("step-length", this.stepLength);
        
        // Standard options to prevent getting stuck
        this.sumoConnection.printSumoOutput(true);
        this.sumoConnection.printSumoError(true);

        // 4. START SERVER AND RUN LOOP
        try {
            System.out.println("⏳ Launching SUMO... (This may pause until TraCI connects)");
            
            // This method BLOCKS until SUMO is open and the TraCI connection is accepted.
            this.sumoConnection.runServer();
            
            System.out.println("✅ Connection established! Starting simulation loop...");
            
            // Once runServer returns, the connection is alive. Now we run the loop.
            runSimulationLoop();
            
            return;

        } catch (IOException e) {
            System.err.println("❌ IO Error: Could not start SUMO or connect.");
            System.err.println("   Check if 'sumo-gui' path is correct and accessible.");
            e.printStackTrace();
            return;
        } catch (Exception e) {
            System.err.println("❌ Runtime Error during simulation.");
            e.printStackTrace();
            return;
        } finally {
            stopSimulation();
        }
    }

    /**
     * Safely resolves the config file path.
     */
    private boolean setupPaths() {
        try {
            URL resource = SimulationManager.class.getClassLoader().getResource(this.sumoConfigFileName);
            
            if (resource == null) {
                System.err.println("❌ CRITICAL: '" + this.sumoConfigFileName + "' not found in resources!");
                return false;
            }

            // FIX: use toURI() to handle spaces/special characters in path correctly
            File file = new File(resource.toURI());
            this.sumoConfigFilePath = file.getAbsolutePath();
            
            // Optional: Check if SUMO binary exists to fail fast
            File sumoBin = new File(this.sumoPath);
            if(!sumoBin.exists() || !sumoBin.canExecute()) {
                System.err.println("❌ CRITICAL: SUMO binary not found or not executable at: " + this.sumoPath);
                return false;
            }
            
            return true;
        } catch (Exception e) {
            System.err.println("❌ Error resolving file paths: " + e.getMessage());
            return false;
        }
    }

    public void runSimulationLoop() {
        // Run for 10000 steps or until connection is lost
        int targetSteps = 1000000;
        
        System.out.println("   -> Running for " + targetSteps + " steps...");
        
        if(!this.sumoConnection.isClosed()) {
        	try {
				Object result = this.sumoConnection.do_job_get(Edge.getIDList());
				@SuppressWarnings("unchecked")
				List<String> currentEdges = (List<String>)result;
				for(String edge:currentEdges) {
					System.out.println(edge);
				}
				System.out.println(currentEdges.size());
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }

//        for (int i = 0; i < targetSteps; i++) {
//            // Safety check to ensure we don't step on a closed connection
//            if (this.sumoConnection.isClosed()) {
//                System.out.println("⚠️ Connection closed unexpectedly.");
//                break;
//            }
//
//            step();
//            
//            // Optional: Print progress every 100 steps
//            if (i % 100 == 0) {
//                System.out.println("   Step: " + i);
//            }
//        }
        System.out.println("✅ Simulation loop finished.");
    }

    public void step() {
        try {
            // 1. Advance Simulation
            this.sumoConnection.do_timestep();
            
            // 2. Update internal state
            this.currentStep++;

            // (Placeholders for your logic)
            // updateActiveVehicles();
            // updateTrafficLights();
            
        } catch (Exception e) {
            System.err.println("❌ Error during timestep " + currentStep);
            e.printStackTrace();
            // If a step fails, we might want to stop the loop
            stopSimulation(); 
        }
    }

    public void stopSimulation() {
        if (this.sumoConnection != null && !this.sumoConnection.isClosed()) {
            this.sumoConnection.close();
            System.out.println("🔌 Connection closed.");
        }
    }

    // --- Getters ---
    public List<SumoVehicle> getActiveVehicles() { return activeVehicles; }
    public StatisticsManager getStatisticsManager() { return statisticsManager; }
    public ReportManager getReportManager() { return reportManager; }
    public int getCurrentStep() { return currentStep; }
    public SumoTraciConnection getConnection() { return sumoConnection; }
}