package model;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import controller.MainController;
import model.infrastructure.*;
import data.SimulationQueue;
import model.infrastructure.*;
import it.polito.appeal.traci.*;
import javafx.scene.control.TextField;
import de.tudresden.sumo.cmd.*;
import de.tudresden.sumo.objects.SumoStage;
import de.tudresden.sumo.objects.SumoStringList;
// Import your vehicle classes
import model.vehicles.VehicleManager;
//import model.vehicles.Car;
//import model.vehicles.Bus;
//import model.vehicles.Truck;
//import model.vehicles.Bike;
import util.Util;
// Import Infrastructure
import model.infrastructure.MapManager;
import model.infrastructure.TrafficlightManager;
import data.*;

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
    private String stepLength = "1"; 

    // --- TraCI Connection ---
    private SumoTraciConnection sumoConnection;

    // --- State Data (The "World") ---
    private	Map<String, EdgeObject> listOfEdges;
	private	Map<String, Map<String, Object>> listOfVehicles;
	private List<String> listOfTrafficlightIds;
	private	Map<String, Map<String, String>> listOfLanes;
	private	Map<String, Map<String, String>> listOfJunctions;
    
    // Sub-Managers & Infrastructure
    private StatisticsManager statisticsManager;
    private ReportManager reportManager;
    private MapManager mapManager; // Holds static map data (Lanes, Edges)
    private VehicleManager vehicleManager;
    private TrafficlightManager trafficlightManager;
    
//    private SimulationQueue queue;
    private SimulationState simulationState;
//	private static int routeCounter = 0;
    private static int vehicleCounter = 0;
	private double standardSpeed = 3.6;
	public boolean isRunning = false;

    // --- Constructor ---
    public SimulationManager(SimulationQueue queue) {
    	this.sumoConnection = new SumoTraciConnection(sumoPath, sumoConfigFileName);
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
            
            if(this.sumoConnection.isClosed()) {
        		System.out.println("Is closed");
        	}
        	else {
        		System.out.println("Is not closed");
        	}
            
            // Load Static Map Data (Edges/Bounds) immediately after connecting
            this.mapManager = new MapManager(sumoConnection);
            this.vehicleManager = new VehicleManager(sumoConnection);
    		this.trafficlightManager = new TrafficlightManager(sumoConnection);
            
            System.out.println("Connection established!");
            this.isRunning = true;
            return true;

        } catch (Exception e) {
            System.err.println("Error starting SUMO: " + e.getMessage());
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
            this.sumoConnection.do_timestep();
//            System.out.println("just did time step");
            this.vehicleManager.step();
            this.simulationState = new SimulationState(this.mapManager.getEdges(),
            										this.vehicleManager.getVehiclesData(),
            										this.trafficlightManager.getTrafficlightData(),
            										this.mapManager.getLaneIdList());
            		
            
        } catch (Exception e) {
            e.printStackTrace();
            stopSimulation(); 
        }
    }

    /** HERE IS THE CODE OF INJECT VEHICLE, STRESSTESTING, FINDR ROUTE **/
    public boolean InjectVehicle(String vehType, int r, int g, int b, int a, double Speed, String firstEdge, String lastEdge) {
		try {
			String routeID = "routes_" + vehicleCounter;			
			SumoStringList edges = getRouteFromEdges(firstEdge, lastEdge, vehType);
			if(edges == null || edges.size() == 0) {
				System.out.println("ERROR: No path found for vehicle type " + vehType + 
						" from edge " + firstEdge + " to edge " + lastEdge);
				return false;
			}
			sumoConnection.do_job_set(Route.add(routeID, edges));
			vehicleManager.injectVehicle(String.valueOf("vehicle_" + vehicleCounter++), vehType, routeID, r, g, b, a, Speed);
		} catch (Exception e){
			System.out.println(e);
		}
		return true;
	}
	
	public void StressTest(int number) throws Exception {
		int N = number;
		String vehicleStringIDs = String.valueOf(sumoConnection.do_job_get(Vehicle.getIDList()));
		List<String> vehicleIDs = Util.parseStringToList(vehicleStringIDs);
		if(vehicleIDs == null || vehicleIDs.size() == 0){
			System.out.println("LOG: PLEASE TRY AGAIN");
			return;
		}
		List<String> randomVehicleIDs = Util.getRandomElementsWithReplacement(vehicleIDs, N);
//		System.out.println(vehicleIDs);
//        System.out.println("Original List Size: " + vehicleIDs.size());
//        System.out.println("Sampled List (N=" + N + "): " + randomVehicleIDs);
		for(int i = 0; i < N; i++) {
			String routeID = "route_" + vehicleCounter;
			SumoStringList edges =  (SumoStringList) sumoConnection.do_job_get(Vehicle.getRoute(randomVehicleIDs.get(i)));
			sumoConnection.do_job_set(Route.add(routeID, edges));
			
			vehicleManager.injectVehicle(String.valueOf("vehicle_" + vehicleCounter++), "DEFAULT_VEHTYPE", routeID, 0, 0, 0, 0, standardSpeed);
		}
	}
	public void StressTest() throws Exception {
		int N = 50;
		String vehicleStringIDs = String.valueOf(sumoConnection.do_job_get(Vehicle.getIDList()));
		List<String> vehicleIDs = Util.parseStringToList(vehicleStringIDs);
		if(vehicleIDs == null || vehicleIDs.size() == 0){
			System.out.println("LOG: PLEASE TRY AGAIN");
			return;
		}
		List<String> randomVehicleIDs = Util.getRandomElementsWithReplacement(vehicleIDs, N);
//		System.out.println(vehicleIDs);
//        System.out.println("Original List Size: " + vehicleIDs.size());
//        System.out.println("Sampled List (N=" + N + "): " + randomVehicleIDs);
		for(int i = 0; i < N; i++) {
			String routeID = "route_" + vehicleCounter;
			SumoStringList edges =  (SumoStringList) sumoConnection.do_job_get(Vehicle.getRoute(randomVehicleIDs.get(i)));
			sumoConnection.do_job_set(Route.add(routeID, edges));
			
			vehicleManager.injectVehicle(String.valueOf("vehicle_" + vehicleCounter++), "DEFAULT_VEHTYPE", routeID, 0, 0, 0, 0, standardSpeed);
		}
	}
	
	public Map<String, EdgeObject> getListOfEdges() {
		return listOfEdges;
	};
	
	public Map<String, Map<String, Object>> getListOfVehicles() {
		return listOfVehicles;
	};
	
	public SumoStringList getRouteFromEdges(String firstEdge, String lastEdge, String vehType) throws Exception {
		double offset = 5;
		double currentTime = (double) sumoConnection.do_job_get(Simulation.getTime());
		double depart = currentTime + offset;
		int routingMode = 0;
		SumoStage stage =  (SumoStage) sumoConnection.do_job_get(Simulation.findRoute(firstEdge, lastEdge, vehType, depart, routingMode));
		SumoStringList edges = stage.edges;
		return edges;
	}

    public void stopSimulation() {
        this.isRunning = false;
        if (this.sumoConnection != null && !this.sumoConnection.isClosed()) {
            this.sumoConnection.close();
            System.out.println("🔌 Connection closed.");
        }
    }

    public boolean setSumoBinary(TextField textField) {
    	String userSumoPath = textField.getText();
    	if(userSumoPath != null && userSumoPath != "") {
    		this.sumoPath = userSumoPath;
    		return true;
    	}
    	return false;
    }
    

    public StatisticsManager getStatisticsManager() { return statisticsManager; }
    public ReportManager getReportManager() { return reportManager; }
//    public int getCurrentStep() { return currentStep; } // Volatile makes this safe
    public SumoTraciConnection getConnection() { return sumoConnection; }
    public MapManager getMapManager() { return mapManager; }
    public SimulationState getState() {
    	return this.simulationState;
    }
}