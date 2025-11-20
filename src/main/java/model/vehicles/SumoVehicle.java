package model.vehicles;
import de.tudresden.sumo.cmd.*;
import de.tudresden.sumo.objects.*;

public abstract class SumoVehicle {
	private String id;
	private SumoPosition2D coordinate;
	private double speed;
	private String color;
	private static int numberOfVehicles;
	
	SumoVehicle(String id){}
	public abstract String getVehicleType();
	public String getID() {}
	public double getSpeed() {}
	public void setPosition(SumoPosition2D coordinate) {}
	public double getX() {}
	public double getY() {}
	
}
