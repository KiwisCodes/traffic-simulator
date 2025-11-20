package model.infrastructure;

import java.util.List;

public class SumoRoute {
    
    private String routeId;
    private List<String> edgeIds; // List of edges that make up the route

    public SumoRoute(String routeId, List<String> edgeIds) {
        this.routeId = routeId;
        this.edgeIds = edgeIds;
    }

    public String getRouteId() { return routeId; }
    public List<String> getEdgeIds() { return edgeIds; }
}