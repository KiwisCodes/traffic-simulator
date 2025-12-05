package model.infrastructure;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import de.tudresden.sumo.cmd.*;
import de.tudresden.sumo.objects.*;
import it.polito.appeal.traci.SumoTraciConnection;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import javafx.scene.paint.Color;




public class TrafficlightManager {
	
	//Changed all TrafficLight to Trafficlight, small l not big l
	//Changed all _ to camel case ex: get_traffic_light_id_list -> getTrafficLightIdList
	
	
	private SumoTraciConnection sumoConnection;
	private List<String> trafficlightIdList = new ArrayList<>();
	private List<TrafficlightObject> trafficlightlinkList = new ArrayList<>();
//	private Map<String, Map<SumoLink, String>> index_map = new HashMap<>(); // map each traffic light (tls_id) to a map that maps the sumolink to the index of that connection
//	private Map<TrafficlightObject, Map<String, String>> drawing_map = new HashMap<>(); // map each traffic light (SumoLink) to the 2 attributes, junction_id and index 
	
	public TrafficlightManager(SumoTraciConnection sumoConnection){
		this.sumoConnection = sumoConnection;
		SumoStringList tlsIdList = new SumoStringList();
		try {
			Object result1 = this.sumoConnection.do_job_get(Trafficlight.getIDList());
			tlsIdList = (SumoStringList) result1;
			this.trafficlightIdList.addAll(tlsIdList);
		}
		catch (Exception e) {
//            alertError("SUMO Traffic Light Connection Failed", e.getMessage());
        }
		
		try {
			for(String i: this.trafficlightIdList) {
				SumoLinkList linkIdList = new SumoLinkList();
				Object result2 = this.sumoConnection.do_job_get(Trafficlight.getControlledLinks(i));
				linkIdList = (SumoLinkList) result2;
//				this.index_map.put(i,  new HashMap<>());
				int run_var = 0;
				for(SumoLink j : linkIdList) {
					TrafficlightObject tmp = new TrafficlightObject(j, i, Integer.toString(run_var));
					String laneId = tmp.get_from_lane_index();

		            if (laneId != null && !laneId.isEmpty()) {
		                Object tmp_obj = sumoConnection.do_job_get(
		                    Lane.getShape(laneId)   // returns list of coordinates
		                );
		                SumoPosition2D posObj = ((SumoGeometry) tmp_obj).coords.getLast();
		                tmp.set_position(posObj);
		            }
		            trafficlightlinkList.add(tmp);
//					this.index_map.get(i).put(j, Integer.toString(run_var));
//					this.drawing_map.put(tmp, new HashMap<>());
//					this.drawing_map.get(tmp).put("index", Integer.toString(run_var));
//					this.drawing_map.get(tmp).put("junction_id", i);
					run_var++;
				}
			}
		}
		catch (Exception e) {
//            alertError("SUMO Traffic Light Connection Failed", e.getMessage());
        }
	}
	
	public List<String> getTrafficlightIdList(){
		return new ArrayList<>(this.trafficlightIdList);
	}
	
	public List<TrafficlightObject> trafficlightlinkList(){
		return new ArrayList<> (this.trafficlightlinkList);
	}


	
//	public List<String> getTrafficlightControlledJunctionsList(String trafficlightId){
//		SumoStringList junctionIdList = new SumoStringList();
//		List<String> final_result = new ArrayList<>();
//		try {
//			Object result = this.sumoConnection.do_job_get(Trafficlight.getControlledJunctions(trafficlightId));
//			junctionIdList = (SumoStringList) result;
//			final_result.addAll(junctionIdList);
//		}
//		catch (Exception e) {
////            alertError("SUMO Traffic Light Get Controlled Junctions Failed", e.getMessage());
//        }
//		return final_result;
//	}
//	
//	public List<SumoLink> getTrafficlightControlledLinksList(String traffic_light_id){
//		List<SumoLink> link_idlist = new ArrayList<SumoLink>(); 
//	    try {
//	        Object result = this.sumoConnection.do_job_get(Trafficlight.getControlledLinks(traffic_light_id));
//	        
//	        // Suppress the warning here, indicating you trust the TraCI protocol
//	        @SuppressWarnings("unchecked")
//	        List<SumoLink> castResult = (List<SumoLink>) result;
//	        
//	        link_idlist.addAll(castResult);
//	    }
//	    catch (Exception e) {
////	        alertError("SUMO Traffic Light Get Controlled Links Failed", e.getMessage());
//	    }
//	    
//	    return link_idlist;
//	}
//	
//	public List<String> getTrafficlightControlledLanesList(String traffic_light_id){
//		SumoStringList lane_idlist = new SumoStringList();
//		List<String> final_result = new ArrayList<>();
//		try {
//			Object result = this.sumoConnection.do_job_get(Trafficlight.getControlledLanes(traffic_light_id));
//			lane_idlist = (SumoStringList) result;
//			final_result.addAll(lane_idlist);
//		}
//		catch (Exception e) {
////            alertError("SUMO Traffic Light Get Controlled Junctions Failed", e.getMessage());
//        }
//		return final_result;
//	}
	
	public Character getCurrentLightState(TrafficlightObject connection) {
		Character output = 'a';
		try {
			Object result = this.sumoConnection.do_job_get(Trafficlight.getRedYellowGreenState(connection.get_host_junction_id()));
			String tmp = (String) result;
			output = tmp.charAt(Integer.parseInt(connection.get_link_index()));
		}
		catch (Exception e) {
//            alertError("SUMO Get Current Light State Failed", e.getMessage());
        }
		return output;
	}
	
	public String getCurrentLightFullState(TrafficlightObject connection) {
		String output = "";
		try {
			Object result = this.sumoConnection.do_job_get(Trafficlight.getRedYellowGreenState(connection.get_host_junction_id()));
			output = (String) result;
		}
		catch (Exception e) {
//            alertError("SUMO Get Current Light State Failed", e.getMessage());
        }
		return output;
	}
	
	public void setCurrentLightState(TrafficlightObject connection, char new_state) {
		String cur_state = this.getCurrentLightFullState(connection);
		StringBuilder sb = new StringBuilder(cur_state);
		sb.setCharAt(Integer.parseInt(connection.get_link_index()), new_state);
		try {
			this.sumoConnection.do_job_set(Trafficlight.setRedYellowGreenState(connection.get_host_junction_id(), sb.toString()));
		}
		catch (Exception e) {
//            alertError("SUMO Set Current Light State Failed", e.getMessage());
        }
		return;
	}
	
	public void setCurrentPhaseDuration(TrafficlightObject connection, double newPhaseDuration) {
		try {
			this.sumoConnection.do_job_set(Trafficlight.setPhaseDuration(connection.get_host_junction_id(), newPhaseDuration));
		}
		catch (Exception e) {
//            alertError("SUMO Set Current Light Phase Duration Failed", e.getMessage());
        }
	}
	
	public Map<TrafficlightObject, Character> getTrafficlightData() {
		Map<TrafficlightObject, Character>result_map = new HashMap<>();
		for(TrafficlightObject i : trafficlightlinkList) {
			
			result_map.put(i, this.getCurrentLightState(i));
		}
		return new HashMap<>(result_map);
	}

//	private void alertError(String title, String msg) {
//        Platform.runLater(() -> {
//            Alert a = new Alert(Alert.AlertType.ERROR, msg);
//            a.setTitle(title); a.show();
//        });
//    }
}