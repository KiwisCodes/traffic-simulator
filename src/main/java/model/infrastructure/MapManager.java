package model.infrastructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.infrastructure.EdgeObject;
import model.infrastructure.LaneObject;
import de.tudresden.sumo.cmd.Edge;
import de.tudresden.sumo.cmd.Junction;
import de.tudresden.sumo.cmd.Lane;
import de.tudresden.sumo.cmd.Simulation;
import de.tudresden.sumo.objects.SumoGeometry;
import de.tudresden.sumo.objects.SumoPosition2D;
import de.tudresden.sumo.util.SumoCommand;
import de.tudresden.ws.container.SumoBoundingBox; // Or use simple doubles
import it.polito.appeal.traci.SumoTraciConnection;

public class MapManager {
//	Sumo Connection
	private SumoTraciConnection sumoConnection;
	private int totalEdge = 0;
	private int totalLane = 0;

//    Static Network Data
    private List<String> edgeIdList;
    private List<String> laneIdList;
    private List<String> junctionIdList;
    private SumoBoundingBox sumoBoundingBox;
    
    private Map<String, EdgeObject> edges;
    private Map<String, LaneObject> lanes;
    private Map<String, JunctionObject> junctions;
    
    
//     Map Dimensions (for the Renderer)
    private double minX = Double.MAX_VALUE;
    private double minY = Double.MAX_VALUE;
    private double maxX = Double.MIN_VALUE;
    private double maxY = Double.MIN_VALUE;

    public MapManager(SumoTraciConnection sumoConnection) {
        if(!sumoConnection.isClosed()) {
        	try {
        		this.sumoConnection = sumoConnection;
				Object response = sumoConnection.do_job_get(Edge.getIDList());
				@SuppressWarnings("unchecked")
				List<String> edges = (List<String>)response;
				this.edgeIdList = edges;
				response = sumoConnection.do_job_get(Junction.getIDList());
				@SuppressWarnings("unchecked")
				List<String> junctions = (List<String>)response;
				this.junctionIdList = junctions;
				response = sumoConnection.do_job_get(Lane.getIDList());
				@SuppressWarnings("unchecked")
				List<String> lanes = (List<String>)response;
				this.laneIdList = lanes;
				response = sumoConnection.do_job_get(Simulation.getNetBoundary());
				
				this.edges = new HashMap<>();
				this.lanes = new HashMap<>();
				this.junctions = new HashMap<>();
				
				
				fetchEdgesFromSumo();
				
				
				
				SumoCommand getNetBoundary = Simulation.getNetBoundary();
				response = this.sumoConnection.do_job_get(getNetBoundary);
				
				if(response instanceof SumoBoundingBox) {
					SumoBoundingBox box = (SumoBoundingBox) response;
					this.maxX = box.x_max;
					this.maxY = box.y_max;
					this.minX = box.x_min;
					this.minY = box.y_min;
				}
				else if(response instanceof SumoGeometry) {
					SumoGeometry geometry = (SumoGeometry) response;
					
					for(SumoPosition2D point : geometry.coords) {
						if(point.x < this.minX) this.minX = point.x;
						if(point.y < this.minY) this.minY = point.y;
						if(point.x > this.maxX) this.maxX = point.x;
						if(point.y > this.maxY) this.maxY = point.y; 
					}
				} else {
				    minX = 0; minY = 0; maxX = 1000; maxY = 1000;
				    System.err.println("Unknown map boundary type: " + response.getClass().getName());
				}
				
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
    public List<String> getEdgeIdList() { 
    	return new ArrayList<> (edgeIdList); 
    }
    public List<String> getLaneIdList() { 
    	return new ArrayList<>(laneIdList); 
    }
    public List<String> getJunctionIdList() { 
    	return new ArrayList<> (junctionIdList);
    }
    
    
    public double getWidth() { return maxX - minX; }
    public double getHeight() { return maxY - minY; }
    public double getMinX() { return minX; }
    public double getMinY() { return minY; }
    public double getMaxX() { return maxX; }
    public double getMaxY() { return maxY; }
    
    
    
    
    //from kkk's
    private void fetchEdgesFromSumo() throws Exception{
    	for(int i = 0; i < this.edgeIdList.size(); i++) {
    		String edgeID = this.edgeIdList.get(i);
    		EdgeObject edge = new EdgeObject(sumoConnection, edgeID);
    		this.edges.put(edgeID, edge);
    		totalEdge++;
    	}
    }
    
    private void fetchLanesFromSumo() throws Exception{
    	//write the laneObject first
    }
    
    private void fetchJunctionsFromSUmo() throws Exception{
    	//write the junctionObject first
    }
    
    public Map<String, EdgeObject> getEdges() throws Exception{
    	//should store this in the attributes, dont need this each step
    	return new HashMap<>(this.edges);
	}
    
    public Map<String, LaneObject> getLanes() throws Exception{
    	return new HashMap<>(this.lanes);
    }
    
    public Map<String, JunctionObject> getJunctions() throws Exception{
    	return new HashMap<>(this.junctions);
    }
    
}