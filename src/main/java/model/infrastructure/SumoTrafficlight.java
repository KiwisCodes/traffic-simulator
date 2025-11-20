package model.infrastructure;

public class SumoTrafficlight {
    
    private String id;
    private String currentState; // e.g., "rGGr"
    private double phaseDuration;

    public SumoTrafficlight(String id) {
        this.id = id;
    }

    public String getId() { return id; }
    
    public String getCurrentState() { return currentState; }
    public void setCurrentState(String currentState) { this.currentState = currentState; }
    
    public double getPhaseDuration() { return phaseDuration; }
    public void setPhaseDuration(double phaseDuration) { this.phaseDuration = phaseDuration; }
}