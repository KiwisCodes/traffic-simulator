package model.infrastructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.infrastructure.EdgeObject;
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
	public int totalEdge = 0;

//    Static Network Data
    private List<String> edgeIdList;
    private List<String> laneIdList;
    private List<String> junctionIds;
//    private List<String> routeIds; this does not exist
    private SumoBoundingBox sumoBoundingBox;
    
    
//     Map Dimensions (for the Renderer)
    private double minX = Double.MAX_VALUE;
    private double minY = Double.MAX_VALUE;
    private double maxX = Double.MIN_VALUE;
    private double maxY = Double.MIN_VALUE;

    public MapManager(SumoTraciConnection sumoConnection) {
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
				
				
				SumoCommand getNetBoundary = Simulation.getNetBoundary();
				Object response = this.sumoConnection.do_job_get(getNetBoundary);
				
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
    public List<String> getEdgeIds() { return edgeIdList; }
    public List<String> getLaneIds() { return new ArrayList<>(laneIdList); }
    public List<String> getJunctionIds() { return junctionIds; }
    public double getWidth() { return maxX - minX; }
    public double getHeight() { return maxY - minY; }
    public double getMinX() { return minX; }
    public double getMinY() { return minY; }
    public double getMaxX() { return maxX; }
    public double getMaxY() { return maxY; }
    
    
    
    
    //from kkk's
    public Map<String, EdgeObject> getEdges() throws Exception{
		Map<String, EdgeObject> edges = new HashMap<>();
		List<String> edgeIDs = (List<String>) sumoConnection.do_job_get(Edge.getIDList());
		for(int i = 0; i < edgeIDs.size(); i++) {
			String edgeID = edgeIDs.get(i);
			EdgeObject edge = new EdgeObject(sumoConnection, edgeID);
			edges.put(edgeID, edge);
			totalEdge++;
		}
		return new HashMap<>(edges);
	}
}