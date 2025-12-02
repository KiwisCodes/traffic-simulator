package controller;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

/**
 * Adds Drag-to-Pan and Scroll-to-Zoom capabilities.
 * It ensures zooming focuses on the mouse cursor position.
 */
public class MapInteractionHandler {
	
	//for the map interaction we have 

    private final Node inputNode;  // The container receiving events (e.g., StackPane/AnchorPane)
    private final Node targetNode; // The map Group that gets moved/scaled
    
    // Panning variables
    private double mouseAnchorX;
    private double mouseAnchorY;
    private double translateAnchorX;
    private double translateAnchorY;

    // Zoom settings
    private static final double MAX_SCALE = 100.0;
    private static final double MIN_SCALE = 0.1;

    public MapInteractionHandler(Node inputNode, Node targetNode) {
        this.inputNode = inputNode;
        this.targetNode = targetNode;
        addListeners();
    }

    private void addListeners() {
        // 1. Mouse Pressed: Record initial position for panning
        inputNode.setOnMousePressed(event -> {
            mouseAnchorX = event.getSceneX();//this function get the current x on the whole scence
            mouseAnchorY = event.getSceneY();
            translateAnchorX = targetNode.getTranslateX();//this func translate the 
            translateAnchorY = targetNode.getTranslateY();//	
            System.out.println(mouseAnchorX);
            System.out.println(mouseAnchorY);
            System.out.println(translateAnchorX);
            System.out.println(translateAnchorY);
            
        });

        // 2. Mouse Dragged: Pan the map
        inputNode.setOnMouseDragged(event -> {
            double deltaX = event.getSceneX() - mouseAnchorX;
            double deltaY = event.getSceneY() - mouseAnchorY;
            
            targetNode.setTranslateX(translateAnchorX + deltaX);
            targetNode.setTranslateY(translateAnchorY + deltaY);
        });

        // 3. Mouse Scroll: Zoom towards the mouse cursor
        inputNode.setOnScroll((ScrollEvent event) -> {
            double zoomFactor = 1.1; // Intensity of zoom
            double deltaY = event.getDeltaY();

            if (deltaY < 0) {
                zoomFactor = 1 / zoomFactor; // Zoom out
            }

            // A. Limit the Zoom (Optional but recommended)
            double currentScale = targetNode.getScaleX();
            double newScale = currentScale * zoomFactor;
            if (newScale > MAX_SCALE || newScale < MIN_SCALE) {
                event.consume();
                return;
            }

            // --- THE FIX: COMPENSATE FOR MOUSE POSITION ---
            
            // B. Get the mouse position relative to the Scene
            double mouseX = event.getSceneX();
            double mouseY = event.getSceneY();

            // C. Find which point on the MAP is currently under the mouse
            Point2D pivotOnMap = targetNode.sceneToLocal(mouseX, mouseY);

            // D. Apply the new Scale
            targetNode.setScaleX(newScale);
            targetNode.setScaleY(newScale);

            // E. Find where that point on the map moved to in the Scene after scaling
            Point2D newLocationInScene = targetNode.localToScene(pivotOnMap);

            // F. Calculate the drift (difference between mouse and new location)
            double driftX = newLocationInScene.getX() - mouseX;
            double driftY = newLocationInScene.getY() - mouseY;

            // G. Adjust the translation to bring that point back under the mouse
            targetNode.setTranslateX(targetNode.getTranslateX() - driftX);
            targetNode.setTranslateY(targetNode.getTranslateY() - driftY);
            
            event.consume();
        });
    }
    
    public void centerMap(Node contentBoundsNode) {
        System.out.println("\n================== MAP CENTERING DEBUG LOG ==================");

        // --- 1. LOG SCALE ---
        double currentScaleX = targetNode.getScaleX();
        double currentScaleY = targetNode.getScaleY();
        System.out.println("CURRENT ZOOM SCALE: " + String.format("%.4f", currentScaleX));

        // --- 2. GET CONTENT BOUNDS (The Roads) ---
        var mapBounds = contentBoundsNode.getLayoutBounds();
        double mapMinX = mapBounds.getMinX();
        double mapMinY = mapBounds.getMinY();
        double mapWidth = mapBounds.getWidth();
        double mapHeight = mapBounds.getHeight();
        double mapMaxX = mapBounds.getMaxX();
        double mapMaxY = mapBounds.getMaxY();

        System.out.println("MAP CONTENT BOUNDS (LanePane):");
        System.out.println("   Min (Top-Left): (" + String.format("%.2f", mapMinX) + ", " + String.format("%.2f", mapMinY) + ")");
        System.out.println("   Max (Bot-Right): (" + String.format("%.2f", mapMaxX) + ", " + String.format("%.2f", mapMaxY) + ")");
        System.out.println("   Size: " + String.format("%.2f", mapWidth) + " x " + String.format("%.2f", mapHeight));

        // CALCULATE MAP CENTER
        double mapCenterX = mapMinX + (mapWidth / 2.0);
        double mapCenterY = mapMinY + (mapHeight / 2.0);
        System.out.println("   CALCULATED MAP CENTER: (" + String.format("%.2f", mapCenterX) + ", " + String.format("%.2f", mapCenterY) + ")");

        // --- 3. GET VIEWPORT BOUNDS (The Screen) ---
        double viewWidth = inputNode.getLayoutBounds().getWidth();
        double viewHeight = inputNode.getLayoutBounds().getHeight();
        
        // Safety check against the Scene size
        if (inputNode.getScene() != null) {
            double sceneHeight = inputNode.getScene().getHeight();
            double sceneWidth = inputNode.getScene().getWidth();
            // Log if we are capping the size
            if (viewHeight > sceneHeight || viewWidth > sceneWidth) {
                 System.out.println("   [NOTICE] StackPane is larger than Scene. Capping dimensions.");
                 viewHeight = Math.min(viewHeight, sceneHeight);
                 viewWidth = Math.min(viewWidth, sceneWidth);
            }
        }

        double paneCenterX = viewWidth / 2.0;
        double paneCenterY = viewHeight / 2.0;

        System.out.println("VIEWPORT (Screen) DETAILS:");
        System.out.println("   Visible Size: " + String.format("%.2f", viewWidth) + " x " + String.format("%.2f", viewHeight));
        System.out.println("   SCREEN CENTER: (" + String.format("%.2f", paneCenterX) + ", " + String.format("%.2f", paneCenterY) + ")");

        // --- 4. CALCULATE DELTA (The movement needed) ---
        // Formula: ScreenCenter - MapCenter
        double deltaX = paneCenterX - mapCenterX;
        double deltaY = paneCenterY - mapCenterY;

        System.out.println("CALCULATED TRANSLATION:");
        System.out.println("   Math says move X by: " + String.format("%.2f", deltaX));
        System.out.println("   Math says move Y by: " + String.format("%.2f", deltaY));

        // --- 5. YOUR MANUAL ADJUSTMENT (For comparison) ---
        double manualX = deltaX; 
        double manualY = deltaY;
        System.out.println("YOUR ADJUSTMENT APPLIED:");
        System.out.println("   Final X: " + String.format("%.2f", manualX));
        System.out.println("   Final Y: " + String.format("%.2f", manualY));

        // Apply the Manual one for now so it looks good while we debug
        targetNode.setTranslateX(manualX);
        targetNode.setTranslateY(manualY);
        
        System.out.println("=============================================================\n");
    }
}