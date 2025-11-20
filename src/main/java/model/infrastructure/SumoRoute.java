package model.infrastructure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a route in the SUMO simulation.
 * A route consists of a sequence of edge IDs.
 */
public class SumoRoute {

    private String id;
    private List<String> edges;
    private String color;       // Optional: for visualization

    public SumoRoute(String id, List<String> edges) {
        this.id = id;
        this.edges = new ArrayList<>(edges); // Defensive copy
        this.color = "yellow"; // Default
    }

    public String getId() {
        return id;
    }

    public List<String> getEdges() {
        return Collections.unmodifiableList(edges);
    }
    
    public String getStartEdge() {
        return edges.isEmpty() ? null : edges.get(0);
    }
    
    public String getEndEdge() {
        return edges.isEmpty() ? null : edges.get(edges.size() - 1);
    }
    
    /**
     * Returns the route as a space-separated string for SUMO commands.
     */
    public String toSumoString() {
        return String.join(" ", edges);
    }
}