package model.vehicles;

import de.tudresden.sumo.objects.SumoColor;
import de.tudresden.ws.container.SumoPosition2D;

public class Vehicle {
    protected String id;
    protected double speed;
    protected SumoPosition2D position;
    protected SumoColor color;
    protected String edgeId;
    protected double angle;
    protected double departureTime;

    public Vehicle(String id, double speed, SumoPosition2D position, SumoColor color, String edgeId, double angle, double departureTime) {
        this.id = id;
        this.speed = speed;
        this.position = position;
        this.color = color;
        this.edgeId = edgeId;
        this.angle = angle;
        this.departureTime = departureTime;
    }

    // --- Getters ---
    public String getId() { return id; }
    public double getSpeed() { return speed; }
    public SumoPosition2D getPosition() { return position; }
    public SumoColor getColor() { return color; }
    public String getEdgeId() { return edgeId; }
    public double getAngle() { return angle; }
    public double getDepartureTime() { return departureTime; }

    // Use this for debugging instead of your print loop
    @Override
    public String toString() {
        return String.format("[%s] ID: %s | Spd: %.2f | Edge: %s", this.getClass().getSimpleName(), id, speed, edgeId);
    }
}