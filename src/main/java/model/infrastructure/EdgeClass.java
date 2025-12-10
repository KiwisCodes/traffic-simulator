package model.infrastructure;

import de.tudresden.sumo.cmd.Edge;
import de.tudresden.sumo.cmd.Lane;
import it.polito.appeal.traci.SumoTraciConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

//public class EdgeObject {
//	private SumoTraciConnection sumoConnection;
//	public String edgeID;
//	public int laneNumber;
//	public boolean allowVehtype;
//	public boolean allowBiketype;
//	public boolean isInternalEdge;
//	public EdgeObject(SumoTraciConnection sumoConnection, String edgeID) throws Exception {
//		this.sumoConnection = sumoConnection;
//		this.edgeID = edgeID;
//		this.laneNumber = (int) sumoConnection.do_job_get(Edge.getLaneNumber(edgeID));
//		getAllowVehicletypes();
//	}
//	
//	
//	public void getAllowVehicletypes() throws Exception{
//		for(int i = 0; i < laneNumber; i++) {
//			String laneID = this.edgeID + "_" + i;
//			this.isInternalEdge = laneID.startsWith(":");//should it be :
//			String allowVehicletypes = String.valueOf(sumoConnection.do_job_get(Lane.getAllowed(laneID)));
//			this.allowBiketype = allowVehicletypes.contains("bicycle");
//			this.allowVehtype = allowVehicletypes.contains("passenger");
//		}
//	}
//	
//}



public class EdgeClass {
    private final SumoTraciConnection sumoConnection;
    private final String edgeId;
    private final int laneCount;


    private final Map<String, LaneClass> lanes;

    //we get this permission by going through each lane, yes i think we dont need that, we can just do sumo do job get but this has better structure
    private boolean allowsPassenger;
    private boolean allowsBicycle;
    private boolean isInternal;

    public EdgeClass(SumoTraciConnection sumoConnection, String edgeId) throws Exception {
        this.sumoConnection = sumoConnection;
        this.edgeId = edgeId;
        this.laneCount = (int) sumoConnection.do_job_get(Edge.getLaneNumber(edgeId));
        this.lanes = new HashMap<>();
        
        fetchLanes();
    }

    private void fetchLanes() throws Exception {
        this.allowsBicycle = false;
        this.allowsPassenger = false;
        this.isInternal = false;

        for (int i = 0; i < this.laneCount; i++) {
            String laneID = this.edgeId + "_" + i;
            LaneClass lane = new LaneClass(sumoConnection, laneID, this.edgeId);
            this.lanes.put(laneID, lane);

            
            if (laneID.startsWith(":")) this.isInternal = true;
            if (lane.isBicycleAllowed()) this.allowsBicycle = true;
            if (lane.isPassengerAllowed()) this.allowsPassenger = true;
        }
    }


    public String getId() { return edgeId; }
    public int getLaneCount() { return laneCount; }
    public boolean isPassengerAllowed() { return allowsPassenger; }
    public boolean isBicycleAllowed() { return allowsBicycle; }
    public boolean isInternal() { return isInternal; }

 
    public Map<String, LaneClass> getLanes() {
        return new HashMap<>(this.lanes);
    }

    @Override
    public String toString() {
//        return String.format("Edge[ID=%s, Lanes=%d, Internal=%b]", edgeId, laneCount, isInternal);
        return "Edge[ID= " + this.edgeId + ", Number of lanes= " + this.getLaneCount() + ", Internal= " + this.isInternal()+ "]";
    }
}