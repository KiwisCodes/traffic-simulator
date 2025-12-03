package model.infrastructure;

import de.tudresden.sumo.cmd.Edge;
import de.tudresden.sumo.cmd.Lane;
import it.polito.appeal.traci.SumoTraciConnection;

public class EdgeObject {
	private SumoTraciConnection sumoConn;
	public String edgeID;
	public int laneNumber;
	public boolean allowVehtype;
	public boolean allowBiketype;
	public boolean isInternalEdge;
	public EdgeObject(SumoTraciConnection sumoConn, String edgeID) throws Exception {
		this.sumoConn = sumoConn;
		this.edgeID = edgeID;
		this.laneNumber = (int) sumoConn.do_job_get(Edge.getLaneNumber(edgeID));
		getAllowVehicletypes();
	}
	
	
	public void getAllowVehicletypes() throws Exception{
		for(int i = 0; i < laneNumber; i++) {
			String laneID = this.edgeID + "_" + i;
			this.isInternalEdge = laneID.startsWith(";");
			String allowVehicletypes = String.valueOf(sumoConn.do_job_get(Lane.getAllowed(laneID)));
			this.allowBiketype = allowVehicletypes.contains("bicycle");
			this.allowVehtype = allowVehicletypes.contains("passenger");
		}
	}
	
}
