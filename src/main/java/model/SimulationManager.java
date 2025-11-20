package model; 

import java.io.IOException;
import java.util.List;

import de.tudresden.sumo.cmd.*; 
import de.tudresden.sumo.cmd.Vehicle;
import de.tudresden.sumo.util.SumoCommand;
import it.polito.appeal.traci.*;

public class SimulationManager {
    private SumoTraciConnection sumoConnection;
    // Stores the String IDs of active vehicles
    private List<String> currentVehicleIDs; 
    private List<String> currentVehicleTypes;
    private List<String> currentRouteIDs;
    
    // ====================================================================
    // 1. MAIN METHOD (Corrected) 🚦
    // ====================================================================
    public static void main(String[] args) {
        SimulationManager manager = new SimulationManager();
        
        // This method handles setup, connection, and runs the entire simulation.
        manager.startSimulation();
    }

    // New wrapper method to organize the simulation process
    private void startSimulation() {
        if (makeTraciConnection()) { // Only proceed if connection is successful
            try {
                // Now proceed to run the simulation loop
            	getVehicleTypes();
                getRoutes();
                for(String vt:currentVehicleTypes) {
               	 System.out.print(vt + " ");
                }
                System.out.println("");
                for(String r:currentRouteIDs) {
               	 System.out.print(r + " ");
                }
                System.out.println("");
                
                String vehID = "MyBike";
                String routeID = this.currentRouteIDs.get(0);
                String typeID = "DEFAULT_BIKETYPE";
                
                int capacity = 0;
                int passengers = 0;

                SumoCommand addBike = Vehicle.addFull(
                    vehID,            // vehID
                    routeID,                   // routeID
                    typeID,         // typeID
                    "0",                       // depart: Time 0 (immediate)
                    "best",                    // departLane: SUMO chooses best lane
                    "0",                       // departPosition: Start of the edge
                    "max",                     // departSpeed: Max speed defined by type
                    "current",                        // arrivalLane: (Default)
                    "random",                        // arrivalPosition: (Default)
                    "current",                        // arrivalSpeed: (Default)
                    "",                        // fromTAZ: (None)
                    "",                        // toTAZ: (None)
                    "",                        // line: (None)
                    capacity,                  // person_capacity: 0
                    passengers                 // person_number: 0
                );
                this.sumoConnection.do_job_set(addBike);
                
            	
                runSimulationLoop(); 
            } catch (Exception e) {
                System.err.println("Critical error during simulation: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    // ===================================================================
    // 2. CONNECTION SETUP 🔗
    // ====================================================================
    private boolean makeTraciConnection() {
        // --- STEP 1 & 2: Define paths and Instantiate ---
        String sumoExecutablePath = "/Users/apple/sumo/bin/sumo-gui"; 
        String sumoCfgFilePath = "/Users/apple/eclipse-workspace/traffic-simulator/src/main/resources/frauasmap.sumocfg"; 
        
        this.sumoConnection = new SumoTraciConnection(sumoExecutablePath, sumoCfgFilePath);
        
        try {
            // Add the option BEFORE runServer()
            sumoConnection.addOption("step-length", "0.001");
            System.out.println("Configured SUMO step length to 0.001s.");
            
            // --- STEP 3: Run the Server ---
            sumoConnection.runServer();
            System.out.println("SUMO server running. Connection established.");
            return true;
            
        } catch (Exception e) {
            System.err.println("Error in TraCI connection setup: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // ====================================================================
    // 3. VEHICLE ID RETRIEVAL (Used during simulation) 🗺️
    // ====================================================================
    // Method now uses instance variable this.sumoConnection
    private void getVehicleIDs() throws Exception {
        Object result = this.sumoConnection.do_job_get(Vehicle.getIDList());
        
        // Correctly cast the result to List<String> (IDs)
        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) result;
        
        this.currentVehicleIDs = ids;
        // System.out.println("Fetched " + this.currentVehicleIDs.size() + " vehicle IDs.");
    }
    
    private void getVehicleTypes() throws Exception {
    	Object result = this.sumoConnection.do_job_get(Vehicletype.getIDList());
    	@SuppressWarnings("unchecked")
    	List<String> ids = (List<String>) result;
    	this.currentVehicleTypes = ids;
    	
    }
    private void getRoutes() throws Exception {
    	Object result = this.sumoConnection.do_job_get(Route.getIDList());
    	@SuppressWarnings("unchecked")
    	List<String> ids = (List<String>) result;
    	this.currentRouteIDs = ids;
    }
    
    
    // ====================================================================
    // 4. SIMULATION LOOP (Main Execution) ⏱️
    // ====================================================================
    private void runSimulationLoop() {
        // 100,000 steps at 0.001s/step = 100 seconds of simulated time.
        int maxSteps = 100000; 
        int step = 0;
        
        try {
            while (step < maxSteps) {
                // --- A. DATA FETCHING (e.g., every 1 second of simulation time) ---
            	
//            	if(step == 10000) {
//            		getVehicleTypes();
//                    getRoutes();
//                	this.sumoConnection.do_job_set(Vehicle.add("150", 
//                			"DEFAULT_BIKETYPE", 
//                			testRoute, 
//                			10,
//                			0,
//                			3.6, 
//                			(byte)0));
//            	}
            	
                if (step % 1000 == 0) { 
                	Object posObj = this.sumoConnection.do_job_get(Vehicle.getPosition("MyBike"));
    			    System.out.println("Vehicle MyBike position: " + posObj);
                    getVehicleIDs();
//                    getVehicleTypes();
//                    getRoutes();
                }
                
//                // --- B. TRAFFIC CONTROL (Example: Speed setting) ---
//                if (currentVehicleIDs != null && !currentVehicleIDs.isEmpty()) {
//                    interactWithVehicles();
//                }

                // --- C. ADVANCE TIME STEP ---
                this.sumoConnection.do_timestep();
                
                if (step % 10000 == 0) {
                     System.out.println("Simulated Time: " + (step * 0.001) + " s | Active Vehicles: " + currentVehicleIDs.size());
                     for(String v:currentVehicleIDs) {
                    	 System.out.print(v + " ");
                     }
                     System.out.println("");
                     for(String vt:currentVehicleTypes) {
                    	 System.out.print(vt + " ");
                     }
                     System.out.println("");
                     
                }
                
                step++;
            }
        } catch (Exception e) {
            System.err.println("Error during simulation loop at step " + step + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Ensure the connection is closed
            if (sumoConnection != null && !sumoConnection.isClosed()) {
                try {
                    sumoConnection.close();
                    System.out.println("SUMO connection successfully closed.");
                } catch (Exception e) {
                    System.err.println("Error closing connection: " + e.getMessage());
                }
            }
        }
    }
    
    // ====================================================================
    // 5. VEHICLE INTERACTION (Example Logic) 🚗
    // ====================================================================
//    private void interactWithVehicles() throws Exception {
//        // Simple example: check the first vehicle and control its speed
//        String targetID = currentVehicleIDs.get(0);
//        
//        // Get current speed
//        double currentSpeed = (double) this.sumoConnection.do_job_get(
//            new Cmd_getVehicleVariable(POI.getSpeed, targetID)
//        );
//        
//        // Control logic: if speed is low, set it higher (15.0 m/s)
//        if (currentSpeed < 10.0) {
//            this.sumoConnection.do_job_set(
//                new Cmd_setVehicleVariable(POI.setSpeed, targetID, 15.0)
//            );
//        }
//    }
}