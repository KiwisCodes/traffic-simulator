package model.vehicles;

import de.tudresden.sumo.cmd.Vehicle;

import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.util.SumoCommand;
import de.tudresden.sumo.objects.SumoStringList;
import de.tudresden.sumo.objects.SumoColor;
import de.tudresden.sumo.config.Constants;
import de.tudresden.sumo.util.SumoCommand; 

import de.tudresden.sumo.cmd.Vehicle;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.IOException;


public class VehicleManager {
	
	private SumoTraciConnection conn;
	private List<String> vehiclesIds;
	private Map<String, Map<String, Object>> vehiclesData;
	
	public VehicleManager(SumoTraciConnection connection) {
		this.conn = connection;
		this.vehiclesData = new HashMap<>();
		
	}
	
	
	public void step() {
		try {
			SumoCommand idListCmd = Vehicle.getIDList();
			
			Object response = this.conn.do_job_get(idListCmd);
			
			if (response instanceof SumoStringList) {
				SumoStringList idList = (SumoStringList) response;
				
				this.vehiclesIds = idList;
				
			}
			this.vehiclesData = new HashMap<>();
			this.updateVehiclesInfo();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void updateVehiclesInfo() {
		
		for (String id: this.vehiclesIds) {
			Map<String, Object> vehicleAttributes = new HashMap<>();
			
			try {
				SumoCommand colorCmd = Vehicle.getColor(id);
				Object colorResponse = this.conn.do_job_get(colorCmd);
				vehicleAttributes.put("Color", colorResponse);
				
				SumoCommand posCmd = Vehicle.getPosition(id);
				Object posResponse = this.conn.do_job_get(posCmd);
				vehicleAttributes.put("Position", posResponse);
				
				SumoCommand speedCmd = Vehicle.getSpeed(id);
				Object speedResponse = this.conn.do_job_get(speedCmd);
				vehicleAttributes.put("Speed", speedResponse);
				
				SumoCommand edgeCmd = Vehicle.getRoadID(id);
				Object edgeResponse = this.conn.do_job_get(edgeCmd);
				vehicleAttributes.put("EdgeId", edgeResponse);
				
				//Hung added
				SumoCommand angleCommand = Vehicle.getAngle(id);
				Object angleResponse = this.conn.do_job_get(angleCommand);
				vehicleAttributes.put("Angle", angleResponse);
				
				SumoCommand departureCmd = new SumoCommand(
						Constants.CMD_GET_VEHICLE_VARIABLE,
						Constants.VAR_DEPARTURE,
						id,
						Constants.RESPONSE_GET_VEHICLE_VARIABLE,
						Constants.TYPE_DOUBLE
				);
				Object departureResponse = this.conn.do_job_get(departureCmd);
				vehicleAttributes.put("Depart", departureResponse);
				
				this.vehiclesData.put(id, vehicleAttributes);
			} catch (Exception e) {
				System.err.println("Error at Request from Vehicle " + id);
				e.printStackTrace();
			}
		}
	}
	
	public Map<String, Map<String, Object>> getVehiclesData() {
		return new HashMap<>(this.vehiclesData);
	}
	
	public void injectVehicle(String vehicleId, String typeId, String routeId, int r, int g, int b, int a, double Speed) {
		try {
			int depart = 0; // depart immediately
			double pos = 0.0;
			byte lane = (byte) 0;
			
			SumoCommand addCmd = Vehicle.add(vehicleId, typeId, routeId, depart, pos, Speed, lane);
			this.conn.do_job_set(addCmd);
			
			SumoColor color = new SumoColor(r, g, b, a);
			
			SumoCommand setColorCmd = Vehicle.setColor(vehicleId, color);
			this.conn.do_job_set(setColorCmd);
			System.out.println("Vehicle Injected: " + vehicleId);

		} catch (Exception e) {
			System.out.println("Error at Injection of Vehicle " + vehicleId);
			e.printStackTrace();
		}
	}
	
	
	public int getVehicleCount() {
		try {
			SumoCommand idCountCmd = Vehicle.getIDCount();
			
			Object response = this.conn.do_job_get(idCountCmd);
			
			int vehicleCount = (Integer) response;
			
			return vehicleCount;
			
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}
	
	
	public void printVehiclesData() {
		if (this.vehiclesData.isEmpty()) {
			System.out.println("No vehicles are active");
			return;
		}
		
		System.out.println("----Actual Vehicles Data----");
		
		for (Map.Entry<String, Map<String, Object>> entry : this.vehiclesData.entrySet()) {
			String carId = entry.getKey();
			Map<String, Object> attributes = entry.getValue();
			
			System.out.println("ID " + carId);
			
			System.out.println(" - Color: " + attributes.get("Color"));
			System.out.println(" - Position: " + attributes.get("Position"));
			System.out.println(" - Speed: " + attributes.get("Speed"));
			
			System.out.println("--------------------------");
		}
	}
	
	public void printIdList(int step) {
		try {
			SumoCommand idListCmd = Vehicle.getIDList();
			
			Object response = this.conn.do_job_get(idListCmd);
			
			if (response instanceof SumoStringList) {
				SumoStringList idList = (SumoStringList) response;
				
				for (String id : idList) {
					System.out.println(id);
				}
	            System.out.println("Step " + step + " Active Vehicles: " + idList.size());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
}