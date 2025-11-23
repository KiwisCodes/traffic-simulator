package model.vehicles;

import de.tudresden.ws.container.SumoPosition2D;

public class VehicleManager {
    
    // Protected fields so subclasses can access if needed, or keep private with getters
    protected String id;
    protected SumoPosition2D position; // Using the TraaS object for coordinates
    protected double speed;
    protected double angle;
    protected String color;
    
    // Static counter if you need to track total instances
    private static int numberOfVehicles = 0;

    public VehicleManager(String id) {
        this.id = id;
        numberOfVehicles++;
    }

    // Abstract method forces subclasses to define their type
    public String getVehicleType() {return "";}	

    // --- Getters and Setters ---
    public String getId() { return id; }
    
    public SumoPosition2D getPosition() { return position; }
    public void setPosition(SumoPosition2D position) { this.position = position; }
    
    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }
    
    public double getAngle() { return angle; }
    public void setAngle(double angle) { this.angle = angle; }
    
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    
    public static int getNumberOfVehicles() { return numberOfVehicles; }
}