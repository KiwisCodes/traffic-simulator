package view;

import java.util.List;

import de.tudresden.sumo.cmd.Edge;
import de.tudresden.sumo.cmd.Lane;
import de.tudresden.sumo.objects.SumoGeometry;
import de.tudresden.sumo.objects.SumoPosition2D;
import de.tudresden.sumo.util.SumoCommand;
import it.polito.appeal.traci.SumoTraciConnection;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import model.infrastructure.SumoMap;
import model.vehicles.*;
import util.CoordinateConverter;

// Helper class to handle the creation of JavaFX shapes, groups
public class Renderer {
	private CoordinateConverter converter;
	
	
	public Renderer(){
		this.converter = new CoordinateConverter();
	}
	
	public void setConverter(SumoMap sumoMap) {
		this.converter.setBound(sumoMap);
	}

    public Shape createVehicleShape(SumoTraciConnection sumoConnection,SumoVehicle vehicle) {
        if (vehicle instanceof Car) {
            return new Circle(4, Color.BLUE);
        } else if (vehicle instanceof Bus) {
            return new Rectangle(12, 5, Color.RED);
        } else if (vehicle instanceof Truck) {
            return new Rectangle(15, 6, Color.ORANGE);
        } else if (vehicle instanceof Bike) {
            return new Circle(2, Color.GREEN);
        }
        // Default
        return new Circle(3, Color.GRAY);
    }
    
    public Group createLaneGroup(SumoTraciConnection sumoConnection, SumoMap sumoMap) {
    	List<String> edges =  sumoMap.getEdgeIds();
    	Group laneGroup =  new Group();
    	try {
	    	for(String edge:edges) {
					int numberOfLanes = (int) sumoConnection.do_job_get(Edge.getLaneNumber(edge));
						for(int i=0; i<numberOfLanes;i++) {
							Polyline singleLaneShape = new Polyline();
							String laneId = edge + "_" + i;
							SumoGeometry geometry = (SumoGeometry) sumoConnection.do_job_get(Lane.getShape(laneId));
							double laneWidth = (double) sumoConnection.do_job_get(Lane.getWidth(laneId));
							for(SumoPosition2D point:geometry.coords) {
								double xScreen = this.converter.toScreenX(point.x);
								double yScreen = this.converter.toScreenY(point.y);
								singleLaneShape.getPoints().addAll(xScreen, yScreen);
							}
							
							singleLaneShape.setStroke(Color.BLACK);
							singleLaneShape.setStrokeWidth(converter.getScale() * laneWidth);
							laneGroup.getChildren().add(singleLaneShape);
						}
	    		
	    	}
    	} catch (Exception e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	}
    	
    	return laneGroup;
    }
    
//implement later
    
//    public Shape createJunctionShape(SumoTraciConnection sumoConnection, SumoMap sumoMap) {
//    	Polyline laneShape = new Polyline();
//    	List<String> junctions =  sumoMap.getJunctionIds();
//    	for(String junction:junctions) {
//    		try {
//				int numberOfLanes = (int) sumoConnection.do_job_get(Edge.getLaneNumber(edge));
//					for(int i=0; i<numberOfLanes;i++) {
//						String laneId = edge + "_" + i;
//						SumoGeometry geometry = (SumoGeometry) sumoConnection.do_job_get(Lane.getShape(laneId));
//						double laneWidth = (double) sumoConnection.do_job_get(Lane.getWidth(laneId));
//					}
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//    		
//    	}
//    	return laneShape;
//    }
    
    
}