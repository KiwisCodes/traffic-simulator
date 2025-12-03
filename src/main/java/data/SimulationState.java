package data;

import java.util.concurrent.atomic.*;

import model.infrastructure.*;

import java.util.*;

public class SimulationState {
	/*
	this is a class that expects copies of data from managers, not reference to those,
	althought the golden rule of java that it always pass by value
	however it is intuitive for primitive data
	as for complex objects, classes, like a Hashmap or a connection or other objects with many attribute
	when you pass those objects into other functions, you are just passing the address of the regions in the heap that
	contains the real value
	so to conclude -> managers must return a copy, not the reference to the real thing.
	*/

	private final Map<String, EdgeObject> lastEdges;
    private final Map<String, Map<String, Object>> lastVehicles;
    private final List<String> laneIdList; //we use this to draw all the lanes;
//    private final List<String> lastTrafficLightIDs; commented all traffic light to test vehicle and edges/lanes
//    private final Map<String, Map<String, String>> lastLanes;
//    private final Map<String, Map<String, String>> lastJunctions;
    public SimulationState(
    		Map<String, EdgeObject> lastEdges,
    		Map<String, Map<String, Object>> lastVehicles,
    		/*List<String> lastTrafficLightIDs*/
    		List<String> laneIdList
    		) 
    {	
    	this.lastEdges = lastEdges;
		this.lastVehicles = lastVehicles;
//		this.lastTrafficLightIDs = lastTrafficLightIDs;
		this.laneIdList = laneIdList;
	}
	public Map<String, EdgeObject> getEdges() { return lastEdges; }
    public Map<String, Map<String, Object>> getVehicles() { return lastVehicles; }
//    public List<String> getTrafficLights() { return lastTrafficLightIDs;}
}
