package controller;

import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

/**
 * Adds Drag-to-Pan and Scroll-to-Zoom capabilities.
 * Attach this to a container to move its content.
 */
public class MapInteractionHandler {

    private final Node inputNode;  // The node that receives mouse events (e.g., the background StackPane)
    private final Node targetNode; // The node that actually moves/zooms (e.g., the Group holding the map)
    
    private double mouseAnchorX;
    private double mouseAnchorY;
    private double translateAnchorX;
    private double translateAnchorY;

    public MapInteractionHandler(Node inputNode, Node targetNode) {
        this.inputNode = inputNode;
        this.targetNode = targetNode;
        addListeners();
    }

    private void addListeners() {
        // 1. Mouse Pressed: Record initial position
        inputNode.setOnMousePressed(event -> {
            mouseAnchorX = event.getSceneX();
            mouseAnchorY = event.getSceneY();
            translateAnchorX = targetNode.getTranslateX();
            translateAnchorY = targetNode.getTranslateY();
        });

        // 2. Mouse Dragged: Update translation (Panning)
        inputNode.setOnMouseDragged(event -> {
            double deltaX = event.getSceneX() - mouseAnchorX;
            double deltaY = event.getSceneY() - mouseAnchorY;
            
            targetNode.setTranslateX(translateAnchorX + deltaX);
            targetNode.setTranslateY(translateAnchorY + deltaY);
        });

        // 3. Mouse Scroll: Update Scale (Zooming)
        inputNode.setOnScroll((ScrollEvent event) -> {
            double zoomFactor = 1.05;
            double deltaY = event.getDeltaY();

            if (deltaY < 0) {
                zoomFactor = 1 / 1.05; // Zoom out
            }

            // Apply scale
            // Note: This zooms towards top-left. For zoom-to-cursor, complex math is needed.
            targetNode.setScaleX(targetNode.getScaleX() * zoomFactor);
            targetNode.setScaleY(targetNode.getScaleY() * zoomFactor);
            
            event.consume();
        });
    }
}