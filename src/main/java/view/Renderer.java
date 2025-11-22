package view;

import java.util.List;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

import model.vehicles.*;

// Helper class to handle the creation of JavaFX shapes
public class Renderer {

    public Shape createVehicleShape(SumoVehicle vehicle) {
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
    
    public Shape createRouteShape(List<String> shapes) {
    	Polyline routeShape = new Polyline();
    	
    	return routeShape;
    }
}