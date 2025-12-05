package model.infrastructure;

import de.tudresden.sumo.cmd.Edge;
import de.tudresden.sumo.cmd.Lane;
import it.polito.appeal.traci.SumoTraciConnection;

public class EdgeObject {
	private SumoTraciConnection sumoConnection;
	public String edgeID;
	public int laneNumber;
	public boolean allowVehtype;
	public boolean allowBiketype;
	public boolean isInternalEdge;
	public EdgeObject(SumoTraciConnection sumoConnection, String edgeID) throws Exception {
		this.sumoConnection = sumoConnection;
		this.edgeID = edgeID;
		this.laneNumber = (int) sumoConnection.do_job_get(Edge.getLaneNumber(edgeID));
		getAllowVehicletypes();
	}
	
	
	public void getAllowVehicletypes() throws Exception{
		for(int i = 0; i < laneNumber; i++) {
			String laneID = this.edgeID + "_" + i;
			this.isInternalEdge = laneID.startsWith(";");
			String allowVehicletypes = String.valueOf(sumoConnection.do_job_get(Lane.getAllowed(laneID)));
			this.allowBiketype = allowVehicletypes.contains("bicycle");
			this.allowVehtype = allowVehicletypes.contains("passenger");
		}
	}
	
}
