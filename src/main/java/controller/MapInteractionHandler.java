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
    
    public void centerMap() {
        // 1. Get Dimensions
        // Target Node (Map Group) size - Use layout bounds for Groups
        double mapWidth = targetNode.getBoundsInLocal().getWidth();
        double mapHeight = targetNode.getBoundsInLocal().getHeight();

        // Input Node (Anchor Pane) size - Use layout bounds for the container
        double paneWidth = inputNode.getBoundsInLocal().getWidth();
        double paneHeight = inputNode.getBoundsInLocal().getHeight();

        // 2. Calculate Translation Offset
        // (Container Size - Map Size) / 2
        double offsetX = (paneWidth - mapWidth) / 2;
        double offsetY = (paneHeight - mapHeight) / 2;

        // 3. Apply Translation
        targetNode.setTranslateX(offsetX);
        targetNode.setTranslateY(offsetY);

        // Update the anchors so the next pan starts from the center position
        // This is optional but good practice if you pan immediately after centering.
        // translateAnchorX = offsetX;
        // translateAnchorY = offsetY;
    }
}