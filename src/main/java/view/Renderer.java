package view;

import java.util.List;
import java.util.function.Consumer; // [NEW] Needed for the callback

import de.tudresden.sumo.cmd.Edge;
import de.tudresden.sumo.cmd.Lane;
import de.tudresden.sumo.objects.SumoGeometry;
import de.tudresden.sumo.objects.SumoPosition2D;
import it.polito.appeal.traci.SumoTraciConnection;
import javafx.scene.Cursor; // [NEW] Change cursor to hand
import javafx.scene.Group;
import javafx.scene.effect.BlurType; // [NEW] For Glow
import javafx.scene.effect.DropShadow; // [NEW] For Glow
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import model.infrastructure.MapManager;
import model.vehicles.*;
import util.CoordinateConverter;

// Helper class to handle the creation of JavaFX shapes, groups
public class Renderer {
    private CoordinateConverter converter;
    
    // [NEW] Define the glow effect once to save memory
    private static final DropShadow HOVER_GLOW = new DropShadow();

    public Renderer(){
        this.converter = new CoordinateConverter();
        
        // [NEW] Initialize the Glow Styling
        HOVER_GLOW.setColor(Color.CYAN);
        HOVER_GLOW.setRadius(15); 
        HOVER_GLOW.setSpread(0.6);
        HOVER_GLOW.setBlurType(BlurType.GAUSSIAN);
    }
    
    public void setConverter(MapManager sumoMap) {
        this.converter.setBound(sumoMap);
    }
    
    public CoordinateConverter getConverter() {
        return this.converter;
    }

    //implement later
//    public Shape createVehicleShape(SumoTraciConnection sumoConnection, Vehicle vehicle) {
//        if (vehicle instanceof Car) {
//            return new Circle(4, Color.BLUE);
//        } else if (vehicle instanceof Bus) {
//            return new Rectangle(12, 5, Color.RED);
//        } else if (vehicle instanceof Truck) {
//            return new Rectangle(15, 6, Color.ORANGE);
//        } else if (vehicle instanceof Bike) {
//            return new Circle(2, Color.GREEN);
//        }
//        // Default
//        return new Circle(3, Color.GRAY);
//    }
    
    /**
     * [UPDATED] Now accepts a 'Consumer' callback to handle clicks
     */
    public Group createLaneGroup(SumoTraciConnection sumoConnection, MapManager sumoMap, Consumer<String> onLaneClick) {
        List<String> edges =  sumoMap.getEdgeIds();
        Group laneGroup =  new Group();
        
        try {
            for(String edge : edges) {
                int numberOfLanes = (int) sumoConnection.do_job_get(Edge.getLaneNumber(edge));
                
                for(int i = 0; i < numberOfLanes; i++) {
                    Polyline singleLaneShape = new Polyline();
                    String laneId = edge + "_" + i;
                    
                    // [NEW] 1. Store the ID so we can retrieve it on click
                    singleLaneShape.setUserData(laneId);
                    
                    SumoGeometry geometry = (SumoGeometry) sumoConnection.do_job_get(Lane.getShape(laneId));
                    double laneWidth = (double) sumoConnection.do_job_get(Lane.getWidth(laneId));
                    
                    for(SumoPosition2D point : geometry.coords) {
                        double xScreen = this.converter.toScreenX(point.x);
                        double yScreen = this.converter.toScreenY(point.y);
                        singleLaneShape.getPoints().addAll(xScreen, yScreen);
                    }
                    
                    // [NEW] 2. Improved Styling
                    Color asphaltColor = Color.rgb(50, 50, 50);
                    singleLaneShape.setStroke(asphaltColor);
                    singleLaneShape.setFill(null); // Important: Remove fill to avoid weird artifacts
                    
                    // Ensure line is at least 3px wide so it is clickable, even if scaled down
                    double visualWidth = converter.getScale() * laneWidth;
                    singleLaneShape.setStrokeWidth(Math.max(3.0, visualWidth));
                    
                    singleLaneShape.setStrokeLineJoin(StrokeLineJoin.ROUND);
                    singleLaneShape.setStrokeLineCap(StrokeLineCap.ROUND);
                    singleLaneShape.setSmooth(true);
                    
                    // [NEW] 3. Add Interaction Listeners
                    
                    // HOVER ENTER
                    singleLaneShape.setOnMouseEntered(e -> {
                        singleLaneShape.setEffect(HOVER_GLOW);       // Turn on glow
                        singleLaneShape.setStroke(Color.LIGHTGRAY);  // Lighten the road
                        singleLaneShape.setCursor(Cursor.HAND);      // Show hand cursor
                        singleLaneShape.toFront(); // Optional: Bring hovered road to top
                    });

                    // HOVER EXIT
                    singleLaneShape.setOnMouseExited(e -> {
                        singleLaneShape.setEffect(null);             // Turn off glow
                        singleLaneShape.setStroke(asphaltColor);     // Reset color
                        singleLaneShape.setCursor(Cursor.DEFAULT);
                    });

                    // CLICK
                    singleLaneShape.setOnMouseClicked(e -> {
                        // Check if a handler was provided
                        if (onLaneClick != null) {
                            String clickedId = (String) singleLaneShape.getUserData();
                            onLaneClick.accept(clickedId);
                        }
                    });
                    
                    laneGroup.getChildren().add(singleLaneShape);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return laneGroup;
    }
    
//implement later
    
//    public Shape createJunctionShape(SumoTraciConnection sumoConnection, SumoMap sumoMap) {
//      // ... (kept as is) ...
//    }
}