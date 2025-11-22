package util;

import javafx.geometry.Point2D;
import model.infrastructure.SumoMap;
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
    
    
    public void setBound(SumoMap map) {
        this.mapMinX = map.getMinX();
        this.mapMaxY = map.getMaxY();
        this.mapWidth = map.getWidth();
        this.mapHeight = map.getHeight();
    }
    

    // ---------------------------------------------------------
    // 1. World (SUMO) -> Screen (JavaFX)
    // ---------------------------------------------------------

    public double toScreenX(double sumoX) {
        // (WorldPos - WorldOrigin) * Zoom + PanOffset
        return (sumoX - mapMinX) * scale + offsetX + padding;
    }

    public double toScreenY(double sumoY) {
        // (WorldCeiling - WorldPos) * Zoom + PanOffset
        // This handles the Flip automatically
        return (mapMaxY - sumoY) * scale + offsetY + padding;
    }

    // Helper for Point objects
    public Point2D toScreen(SumoPosition2D sumoPoint) {
        return new Point2D(toScreenX(sumoPoint.x), toScreenY(sumoPoint.y));
    }

    // ---------------------------------------------------------
    // 2. Screen (JavaFX) -> World (SUMO)
    // Useful for: Clicking on a car with the mouse
    // ---------------------------------------------------------

    public double toSumoX(double screenX) {
        return ((screenX - padding - offsetX) / scale) + mapMinX;
    }

    public double toSumoY(double screenY) {
        return mapMaxY - ((screenY - padding - offsetY) / scale);
    }

    // ---------------------------------------------------------
    // 3. View Management (Zooming & Panning)
    // ---------------------------------------------------------

    /**
     * Automatically calculates the scale needed to fit the map 
     * inside the given window size.
     */
    
    //does not support default arguments
    public void autoFitToWindow() {
        double workableWidth = this.windowWidth - (padding * 2);
        double workableHeight = this.windowHeight - (padding * 2);

        double scaleX = workableWidth / mapWidth;
        double scaleY = workableHeight / mapHeight;

        // Choose the smaller scale so the whole map fits
        this.scale = Math.min(scaleX, scaleY);
        
        // Reset pan to center
        this.offsetX = 0;
        this.offsetY = 0;
        
        System.out.println("Auto-scaled map to: " + this.scale);
    }

    public void zoom(double factor) {
        this.scale *= factor;
    }

    public void setPan(double x, double y) {
        this.offsetX = x;
        this.offsetY = y;
    }
    
    public double getScale() { return scale; }
}