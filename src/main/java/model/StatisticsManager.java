package model;

import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.util.SumoCommand;
import de.tudresden.sumo.objects.SumoStringList;
import de.tudresden.sumo.objects.SumoColor;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.Map;
import java.io.IOException;

public class StatisticsManager {
	private Map<String, Map<String, Object>> vehiclesData;
	private int step;
	
	public StatisticsManager() {
		this.vehiclesData = new HashMap<>();
	}
	
	public void step(Map<String, Map<String, Object>> vehiclesInfo, int step) {
		this.vehiclesData = vehiclesInfo;
		this.step = step;
		return;
	}
	
	// Calculate average speed of vehicles
	public double avgVehiclesSpeed() {
		if (this.vehiclesData.isEmpty()) {
			System.err.println("There are no vehicles currently");
			return 0.0;
		}
		
		double totalSpeed = 0;
		for (Map.Entry<String, Map<String, Object>> entry: this.vehiclesData.entrySet()) {
			Map<String, Object> attributes = entry.getValue();
			double speed = (double)attributes.get("Speed");
			totalSpeed = totalSpeed + speed;
		}
		return totalSpeed / vehiclesData.size() ;
	}
	
	
	// Find out congestion spots
	public List<String> findCongestionSpots() {
		Map<String, List<Double>> edgeSpeeds = new HashMap<>();
		
		for(Map<String, Object> attributes: this.vehiclesData.values()) {
			String edgeId = (String) attributes.get("EdgeId");
			double speed = (double)attributes.get("Speed");
			
			edgeSpeeds.putIfAbsent(edgeId, new ArrayList<>());
			edgeSpeeds.get(edgeId).add(speed);
		}
		
		List<String> congestedEdges = new ArrayList<>();
		double congestionThreshold = 5.0;
		
		for (Map.Entry<String, List<Double>> entry : edgeSpeeds.entrySet()) {
			String edgeId = entry.getKey();
			List<Double> speeds = entry.getValue();
			
			double sum = 0;
			
			for (Double s : speeds) {
				sum += s;
			}
			
			double avg = sum / speeds.size();
			
			if (avg < congestionThreshold) {
				congestedEdges.add(edgeId);
			}
		}
		
		return congestedEdges;
	}
	
	
	
	
	// Calculate the vehicle density per edge
	public Map<String, Integer> calculateVehicleDensity() {
		Map<String, Integer> densityMap = new HashMap<>();
		
		if (this.vehiclesData.isEmpty()) {
			return densityMap;
		}
		
		for (Map<String, Object> attributes : this.vehiclesData.values()) {
			String edgeId = (String) attributes.get("EdgeId");
			
			densityMap.put(edgeId, densityMap.getOrDefault(edgeId, 0) + 1);
		}
		
		return densityMap;
	}
	
	
	
	// Calculate travel time distribution
	public Map<String, Integer> calculateTravelTimeDistribution(int binSizeSeconds) {
		Map<String, Integer> distribution = new TreeMap<>((a, b) -> {
			int lowerA = Integer.parseInt(a.split("-")[0]);
			int lowerB = Integer.parseInt(b.split("-")[0]);
			return Integer.compare(lowerA, lowerB);
		});
		
		if (this.vehiclesData.isEmpty()) {
			return distribution;
		}
		
		
		for (Map.Entry<String, Map<String, Object>> entry : this.vehiclesData.entrySet()) {
			Map<String, Object> attributes = entry.getValue();
			double departureTime = (double)attributes.get("Depart");			
			double currentTravelTime = this.step - departureTime;
//			System.out.println("Vehicle" + entry.getKey() + " " + currentTravelTime);
			if (currentTravelTime < 0) {
				continue;
			}
			int binIndex = (int) (currentTravelTime / binSizeSeconds);
			int lowerBound = binIndex * binSizeSeconds;
			int upperBound = (binIndex + 1) * binSizeSeconds;
//			System.out.println("Current Travel Time: " + currentTravelTime);
			
			String key = lowerBound + "-" + upperBound;
			distribution.put(key, distribution.getOrDefault(key, 0) + 1);
		}
		return distribution;
	}
	
}