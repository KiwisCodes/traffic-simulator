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
    

    // ---------------------------------------------------------
    // 1. World (SUMO) -> Screen (JavaFX)
    // ---------------------------------------------------------

    public double toScreenX(double sumoX) {
        // (WorldPos - WorldOrigin) * Zoom + PanOffset
        return (sumoX - mapMinX) * scale + offsetX;
    }

    public double toScreenY(double sumoY) {
        // (WorldCeiling - WorldPos) * Zoom + PanOffset
        // This handles the Flip automatically
        return (mapMaxY - sumoY) * scale + offsetY;
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
        return ((screenX - offsetX) / scale) + mapMinX;
    }

    public double toSumoY(double screenY) {
        return mapMaxY - ((screenY - offsetY) / scale);
    }

    // ---------------------------------------------------------
    // 3. View Management (Zooming & Panning)
    // ---------------------------------------------------------

    /**
     * Automatically calculates the scale needed to fit the map 
     * inside the given window size.
     */
    
    //does not support default arguments
 // Update in util/CoordinateConverter.java

 // Add this method to dynamically set the canvas size
    public void autoFit(double paneWidth, double paneHeight) {


        // 1. SAFETY CHECK: If window size is invalid, use a default
        if (paneWidth <= padding * 2 || paneHeight <= padding * 2) {
            System.out.println("Window not ready yet. Using default size.");
            paneWidth = 1000;  // Default fallback width
            paneHeight = 800;  // Default fallback height
        }

        double mapW = this.mapWidth;
        double mapH = this.mapHeight;

        // 2. Prevent Divide by Zero if map failed to load
        if (mapW <= 0 || mapH <= 0) {
            System.err.println("Map dimensions are zero! Cannot auto-fit.");
            return; 
        }

        // 3. Calculate Scale
        double scaleX = (paneWidth - (padding * 2)) / mapW;
        double scaleY = (paneHeight - (padding * 2)) / mapH;

        this.scale = Math.min(scaleX, scaleY);
        
        // 4. Calculate Center Offsets
        double usedWidth = mapW * scale;
        double usedHeight = mapH * scale;

        this.offsetX = (paneWidth - usedWidth) / 2;
        this.offsetY = (paneHeight - usedHeight) / 2;
        
        System.out.println("AutoFit Calculated -> Scale: " + this.scale + " OffsetX: " + offsetX + " OffsetY: " + offsetY);
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