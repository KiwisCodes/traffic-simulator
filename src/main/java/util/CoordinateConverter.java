package util;

import javafx.geometry.Point2D;
import model.infrastructure.MapManager;
import view.MainGUI;
import de.tudresden.ws.container.SumoPosition2D; 


public class CoordinateConverter {

    // --- Static World Data (From SumoMap) ---
    private double mapMinX;
    private double mapMaxY; // The "Ceiling" of the map
    private double mapWidth;
    private double mapHeight;

    // --- Dynamic View Data (Zoom & Pan) ---
    private double scale = 1;     // Pixels per Meter
    private double offsetX = 0.0;   // Panning X
    private double offsetY = 0.0;   // Panning Y
    private final double padding = 50.0; // Empty space around map edges
    
    private int windowWidth = MainGUI.windowWidth;
    private int windowHeight = MainGUI.windowHeight;
    

    // Constructor: Locks onto a specific Map
    public CoordinateConverter() {
    	
    }
    
    
    public void setBound(MapManager map) {
        this.mapMinX = map.getMinX();
        this.mapMaxY = map.getMaxY();
        this.mapWidth = map.getWidth();
        this.mapHeight = map.getHeight();
    }


    public double toScreenX(double sumoX) {
        return (sumoX - mapMinX) * scale + offsetX;
    }

    public double toScreenY(double sumoY) {
        return (mapMaxY - sumoY) * scale + offsetY;
    }

    public Point2D toScreen(SumoPosition2D sumoPoint) {
        return new Point2D(toScreenX(sumoPoint.x), toScreenY(sumoPoint.y));
    }

    public double toSumoX(double screenX) {
        return ((screenX - offsetX) / scale) + mapMinX;
    }

    public double toSumoY(double screenY) {
        return mapMaxY - ((screenY - offsetY) / scale);
    }
    
    
    public void setPan(double x, double y) {
        this.offsetX = x;
        this.offsetY = y;
    }
    
    public double getScale() { return scale; }
}