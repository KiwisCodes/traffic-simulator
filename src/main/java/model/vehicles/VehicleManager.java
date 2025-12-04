package model.vehicles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.tudresden.sumo.cmd.Vehicle;
import de.tudresden.sumo.objects.SumoColor;
import de.tudresden.sumo.objects.SumoStringList;
import de.tudresden.sumo.util.SumoCommand;
import de.tudresden.ws.container.SumoPosition2D;
import it.polito.appeal.traci.SumoTraciConnection;
import util.Util;

public class VehicleManager {
	
	//why Map<String, Object> are they not String, if String then get rid of Object;
	//changed all conn to sumoConnection
	//changed all Cmd to Command
	private SumoTraciConnection sumoConnection;
	private List<String> vehiclesIds;
	private Map<String, Map<String, Object>> vehiclesData;
	
	public VehicleManager(SumoTraciConnection sumoConnection) {
		
		this.sumoConnection = sumoConnection;
		this.vehiclesData = new HashMap<>();
		
	}
	
//	public void step() {
//		try {
//			SumoCommand idListCommand = Vehicle.getIDList();
//			
//			Object response = this.sumoConnection.do_job_get(idListCommand);
//			
//			
//			if (response instanceof SumoStringList) {
//				SumoStringList idList = (SumoStringList) response;
//				
//				this.vehiclesIds = idList;
//			}
//			for(String id:this.vehiclesIds) System.out.print(id + " ");
//			this.updateVehiclesInfo();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
	
	public void step() {
        // FIX 2: Check if connection is alive before talking to SUMO
        if (this.sumoConnection.isClosed()) {
        	System.out.println("did not step in vehicle");
            return;
        }

        try {
            SumoCommand idListCommand = Vehicle.getIDList();
            Object response = this.sumoConnection.do_job_get(idListCommand);

            if (response instanceof SumoStringList) {
                SumoStringList idList = (SumoStringList) response;
                this.vehiclesIds = idList;
            } else {
                // If we didn't get a list, clear our local list to be safe
                this.vehiclesIds.clear();
            }

            // FIX 3: Clear old data so cars that left the map don't stay on screen
            this.vehiclesData.clear(); 
            
//            for(String id:this.vehiclesIds) System.out.print(id + " ");
            
            // Now update the info for the current cars
            this.updateVehiclesInfo();
            

        } catch (Exception e) {
            System.err.println("Error in VehicleManager step: " + e.getMessage());
            e.printStackTrace();
        }
    }
	
	public void updateVehiclesInfo() {
		
		for (String id: this.vehiclesIds) {
			Map<String, Object> vehicleAttributes = new HashMap<>();
			
			try {
				SumoCommand colorCommand = Vehicle.getColor(id);
				Object colorResponse = this.sumoConnection.do_job_get(colorCommand);
				vehicleAttributes.put("Color", colorResponse);
				SumoCommand posCommand = Vehicle.getPosition(id);
				Object posResponse = this.sumoConnection.do_job_get(posCommand);
				vehicleAttributes.put("Position", posResponse);
				
				SumoCommand speedCommand = Vehicle.getSpeed(id);
				Object speedResponse = this.sumoConnection.do_job_get(speedCommand);
				vehicleAttributes.put("Speed", speedResponse);
				
				//Hung added
				SumoCommand angleCommand = Vehicle.getAngle(id);
				Object angleResponse = this.sumoConnection.do_job_get(angleCommand);
				vehicleAttributes.put("Angle", angleResponse);
//				System.out.println("ID: " + id + "Color: " + String.valueOf(colorResponse));
				
				this.vehiclesData.put(id, vehicleAttributes);
			} catch (Exception e) {
//				System.err.println("Error at Request from Vehicle " + id);
				System.err.println(e);
			}
		}
	}
	
	public Map<String, Map<String, Object>> getVehiclesData() {
		System.out.println("Return vehicle map");
		return new HashMap<>(this.vehiclesData);
	}
	
	public void injectVehicle(String vehicleId, String typeId, String routeId, int r, int g, int b, int a, double Speed) {
		try {
			int depart = Util.getDepartTime(sumoConnection); // depart immediately
			double pos = 0.0;
			byte lane = (byte) 0;
			
			SumoCommand addCommand = Vehicle.add(vehicleId, typeId, routeId, depart, pos, Speed, lane);
			this.sumoConnection.do_job_set(addCommand);
			
			SumoColor color = new SumoColor(r, g, b, a);
			
			SumoCommand setColorCommand = Vehicle.setColor(vehicleId, color);
			this.sumoConnection.do_job_set(setColorCommand);
			System.out.println("Vehicle Injected: " + vehicleId);

		} catch (Exception e) {
			System.out.println("Error at Injection of Vehicle " + vehicleId);
			e.printStackTrace();
		}
	}
	
	
	public int getVehicleCount() {
		try {
			SumoCommand idCountCommand = Vehicle.getIDCount();
			
			Object response = this.sumoConnection.do_job_get(idCountCommand);
			
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
			SumoCommand idListCommand = Vehicle.getIDList();
			
			Object response = this.sumoConnection.do_job_get(idListCommand);
			
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
	
	
	public static void main(String[] args) {
		
	}
}