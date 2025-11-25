package model.infrastructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.tudresden.sumo.cmd.Edge;
import de.tudresden.sumo.cmd.Junction;
import de.tudresden.sumo.cmd.Lane;
import de.tudresden.sumo.cmd.Simulation;
import de.tudresden.ws.container.SumoBoundingBox; // Or use simple doubles
import it.polito.appeal.traci.SumoTraciConnection;

public class MapManger {
//	Sumo Connection
	private SumoTraciConnection sumoConnection;
//    Static Network Data
    private List<String> edgeIdList;
    private List<String> laneIdList;
    private List<String> junctionIds;
//    private List<String> routeIds; this does not exist
    private SumoBoundingBox sumoBoundingBox;
    
    
//     Map Dimensions (for the Renderer)
    private double minX;
    private double minY;
    private double maxX;
    private double maxY;

    public MapManger(SumoTraciConnection sumoConnection) {
        if(!sumoConnection.isClosed()) {
        	try {
        		this.sumoConnection = sumoConnection;
				Object result = sumoConnection.do_job_get(Edge.getIDList());
				@SuppressWarnings("unchecked")
				List<String> edges = (List<String>)result;
				this.edgeIdList = edges;
				result = sumoConnection.do_job_get(Junction.getIDList());
				@SuppressWarnings("unchecked")
				List<String> junctions = (List<String>)result;
				this.junctionIds = junctions;
				result = sumoConnection.do_job_get(Lane.getIDList());
				@SuppressWarnings("unchecked")
				List<String> lanes = (List<String>)result;
				this.laneIdList = lanes;
				result = sumoConnection.do_job_get(Simulation.getNetBoundary());
				@SuppressWarnings("deprecation")
				SumoBoundingBox sumoBoundingBox = (SumoBoundingBox) result;
				this.sumoBoundingBox = sumoBoundingBox;
				this.minX = this.sumoBoundingBox.x_min;
				this.minY = this.sumoBoundingBox.y_min;
				this.maxX = this.sumoBoundingBox.x_max;
				this.maxY = this.sumoBoundingBox.y_max;
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }

//     Setters (Called by SimulationManager when loading)
    public void setEdgeIds(List<String> edges) { this.edgeIdList = edges; }
    public void setBounds(double minX, double minY, double maxX, double maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

//     Verification Helpers
    public boolean isValidEdge(String id) {
        return edgeIdList.contains(id);
    }

    
    
    
    
    

//    Getters
    public List<String> getEdgeIds() { return edgeIdList; }
    public List<String> getLaneIds() { return laneIdList; }
    public List<String> getJunctionIds() { return junctionIds; }
    public double getWidth() { return maxX - minX; }
    public double getHeight() { return maxY - minY; }
    public double getMinX() { return minX; }
    public double getMinY() { return minY; }
    public double getMaxX() { return maxX; }
    public double getMaxY() { return maxY; }
    
    
    
    
    //from kkk's
    public Map<String, Map<String, String>> getEdges() throws Exception{
		Map<String, Map<String, String>> edges = new HashMap<>();
		Object result = this.sumoConnection.do_job_get(Edge.getIDList());
		List<String> edgeIdList = (List<String>) result;
		this.edgeIdList = edgeIdList;
		
		for(int i = 0; i < this.edgeIdList.size(); i++) {
			String edgeID = this.edgeIdList.get(i);
			Map<String, String> edgeInfos = new HashMap<>();
			
			edgeInfos.put("CO2Emission", String.valueOf(sumoConnection.do_job_get(Edge.getCO2Emission(edgeID))));
			edgeInfos.put("COEmission", String.valueOf(sumoConnection.do_job_get(Edge.getCOEmission(edgeID))));
			edgeInfos.put("electricityConsumption", String.valueOf(sumoConnection.do_job_get(Edge.getElectricityConsumption(edgeID))));
			edgeInfos.put("fuelConsumption", String.valueOf(sumoConnection.do_job_get(Edge.getFuelConsumption(edgeID))));
			edgeInfos.put("HCEmission", String.valueOf(sumoConnection.do_job_get(Edge.getHCEmission(edgeID))));
			edgeInfos.put("laneNumber", String.valueOf(sumoConnection.do_job_get(Edge.getLaneNumber(edgeID))));
			edgeInfos.put("lastStepHaltingNumber", String.valueOf(sumoConnection.do_job_get(Edge.getLastStepHaltingNumber(edgeID))));
			edgeInfos.put("lastStepLength", String.valueOf(sumoConnection.do_job_get(Edge.getLastStepLength(edgeID))));
			edgeInfos.put("lastStepMeanSpeed", String.valueOf(sumoConnection.do_job_get(Edge.getLastStepMeanSpeed(edgeID))));
			edgeInfos.put("lastStepOccupancy", String.valueOf(sumoConnection.do_job_get(Edge.getLastStepOccupancy(edgeID))));
			edgeInfos.put("lastStepVehicleNumber", String.valueOf(sumoConnection.do_job_get(Edge.getLastStepVehicleNumber(edgeID))));
			edgeInfos.put("NOxEmission", String.valueOf(sumoConnection.do_job_get(Edge.getNOxEmission(edgeID))));
			edgeInfos.put("NoiseEmission", String.valueOf(sumoConnection.do_job_get(Edge.getNoiseEmission(edgeID))));
			edgeInfos.put("PMxEmission", String.valueOf(sumoConnection.do_job_get(Edge.getPMxEmission(edgeID))));
			edgeInfos.put("travelTime", String.valueOf(sumoConnection.do_job_get(Edge.getTraveltime(edgeID))));
			edgeInfos.put("waitingTime", String.valueOf(sumoConnection.do_job_get(Edge.getWaitingTime(edgeID))));
			
			edges.put(edgeID, edgeInfos);
		}
		
		return edges;
	}
	
	
	//not done
	public Map<String, Map<String, String>> getLanes() throws Exception{
		Map<String, Map<String, String>> lanes = new HashMap<>();
		
		if(this.edgeIdList == null) {
			Object result = this.sumoConnection.do_job_get(Edge.getIDList());
			List<String> edgeIdList = (List<String>) result;
			this.edgeIdList = edgeIdList;
		}
		
		for(int i = 0; i < this.edgeIdList.size(); i++) {
			String edgeID = this.edgeIdList.get(i);
			Map<String, String> edgeInfos = new HashMap<>();
			
		}
		
		return lanes;
	}
}