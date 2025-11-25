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


public class TrafficlightManager {
	
	//Changed all TrafficLight to Trafficlight, small l not big l
	//Changed all _ to camel case ex: get_traffic_light_id_list -> getTrafficLightIdList
	
	
	private SumoTraciConnection sumoConnection;
	private List<String> trafficlightIdList = new ArrayList<>();
	
	public TrafficlightManager(SumoTraciConnection sumoConnection){
		this.sumoConnection = sumoConnection;
		List<String> tlsIdList = new ArrayList();
		try {
			Object result = this.sumoConnection.do_job_get(Trafficlight.getIDList());
			tlsIdList = (List<String>) result;
		}
		catch (Exception e) {
//            alertError("SUMO Traffic Light Connection Failed", e.getMessage());
        }
		this.trafficlightIdList.addAll(tlsIdList);
//		for(String i: tlsIdList)
//			System.out.println(i);
	}
	
	public List<String> getTrafficlightIdList(){
		return trafficlightIdList;
	}
	
	public List<String> getTrafficlightControlledJunctionsList(String trafficlightId){
		List<String> junctionIdList = new ArrayList();
		try {
			Object result = this.sumoConnection.do_job_get(Trafficlight.getControlledJunctions(trafficlightId));
			junctionIdList = (List<String>) result;
		}
		catch (Exception e) {
//            alertError("SUMO Traffic Light Get Controlled Junctions Failed", e.getMessage());
        }
		return junctionIdList;
	}
	
	public String getCurrentLightState(String trafficlightId) {
		String outputList = "";
		try {
			Object result = this.sumoConnection.do_job_get(Trafficlight.getRedYellowGreenState(trafficlightId));
			outputList = (String) result;
		}
		catch (Exception e) {
//            alertError("SUMO Get Current Light State Failed", e.getMessage());
        }
		return outputList;
	}
	
	public void setCurrentLightState(String trafficlightId,String new_state) {
		try {
			this.sumoConnection.do_job_set(Trafficlight.setRedYellowGreenState(trafficlightId, new_state));
		}
		catch (Exception e) {
//            alertError("SUMO Set Current Light State Failed", e.getMessage());
        }
		return;
	}
	
	public double getCurrentPhaseDuration(String trafficlightId) {
		double output = 0;
		try {
			Object result = this.sumoConnection.do_job_get(Trafficlight.getPhaseDuration(trafficlightId));
			output = (double) result;
		}
		catch (Exception e) {
//            alertError("SUMO Get Current Light Phase Duration Failed", e.getMessage());
        }
		return output;
	}
	
	public void setCurrentPhaseDuration(String trafficlightId, double newPhaseDuration) {
		try {
			this.sumoConnection.do_job_set(Trafficlight.setPhaseDuration(trafficlightId, newPhaseDuration));
		}
		catch (Exception e) {
//            alertError("SUMO Set Current Light Phase Duration Failed", e.getMessage());
        }
	}
	
	public double getNextSwitch(String trafficlightId) {
		double output = 0;
		try {
			Object result = this.sumoConnection.do_job_get(Trafficlight.getNextSwitch(trafficlightId));
			output = (double) result;
		}
		catch (Exception e) {
//            alertError("SUMO Get Current Light State Failed", e.getMessage());
        }
		return output;
	}
	
//	private void alertError(String title, String msg) {
//        Platform.runLater(() -> {
//            Alert a = new Alert(Alert.AlertType.ERROR, msg);
//            a.setTitle(title); a.show();
//        });
//    }
}
