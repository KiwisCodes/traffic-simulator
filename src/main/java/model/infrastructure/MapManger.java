package model.infrastructure;

import java.util.ArrayList;
import java.util.List;

import de.tudresden.sumo.cmd.Edge;
import de.tudresden.sumo.cmd.Junction;
import de.tudresden.sumo.cmd.Lane;
import de.tudresden.sumo.cmd.Simulation;
import de.tudresden.ws.container.SumoBoundingBox; // Or use simple doubles
import it.polito.appeal.traci.SumoTraciConnection;

public class MapManger {

    // --- Static Network Data ---
    private List<String> edgeIds;
    private List<String> laneIds;
    private List<String> junctionIds;
    private List<String> routeIds;
    private SumoBoundingBox sumoBoundingBox;
    
    
    // --- Map Dimensions (Useful for Renderer) ---
    private double minX;
    private double minY;
    private double maxX;
    private double maxY;

    public MapManger(SumoTraciConnection sumoConnection) {
        if(!sumoConnection.isClosed()) {
        	try {
				Object result = sumoConnection.do_job_get(Edge.getIDList());
				@SuppressWarnings("unchecked")
				List<String> edges = (List<String>)result;
				this.edgeIds = edges;
				result = sumoConnection.do_job_get(Junction.getIDList());
				@SuppressWarnings("unchecked")
				List<String> junctions = (List<String>)result;
				this.junctionIds = junctions;
				result = sumoConnection.do_job_get(Lane.getIDList());
				@SuppressWarnings("unchecked")
				List<String> lanes = (List<String>)result;
				this.laneIds = lanes;
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

    // --- Setters (Called by SimulationManager when loading) ---
    public void setEdgeIds(List<String> edges) { this.edgeIds = edges; }
    public void updateRouteIds(List<String> routes) { this.routeIds = routes; }
    public void setBounds(double minX, double minY, double maxX, double maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    // --- Verification Helpers ---
    public boolean isValidEdge(String id) {
        return edgeIds.contains(id);
    }

    public boolean isValidRoute(String id) {
        return routeIds.contains(id);
    }

    // --- Getters ---
    public List<String> getEdgeIds() { return edgeIds; }
    public List<String> getLaneIds() { return edgeIds; }
    public List<String> getJunctionIds() { return junctionIds; }
    public List<String> getRouteIds() { return routeIds; }
    public double getWidth() { return maxX - minX; }
    public double getHeight() { return maxY - minY; }
    public double getMinX() { return minX; }
    public double getMinY() { return minY; }
    public double getMaxX() { return maxX; }
    public double getMaxY() { return maxY; }
}