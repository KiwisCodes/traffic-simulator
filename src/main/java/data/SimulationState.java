package data;

import java.util.concurrent.atomic.*;
import java.util.*;

public class SimulationState {
	private final Map<String, Map<String, String>> lastEdges;
    private final Map<String, Map<String, Object>> lastVehicles;
    private final List<String> lastTrafficLightIDs;
    public SimulationState(Map<String, Map<String, String>> lastEdges, Map<String, Map<String, Object>> lastVehicles, List<String> lastTrafficLightIDs) {	
    	this.lastEdges = lastEdges;
		this.lastVehicles = lastVehicles;
		this.lastTrafficLightIDs = lastTrafficLightIDs;
	}
	public Map<String, Map<String, String>> getEdges() { return lastEdges; }
    public Map<String, Map<String, Object>> getVehicles() { return lastVehicles; }
    public List<String> getTrafficLights() { return lastTrafficLightIDs; }
}
