package model;

public class StatisticsManager {
    
    private double averageSpeed;
    private int totalVehicles;
    private int totalTrafficlights;
    private double averageTravelTime;
    private int congestionCount;
    
    public StatisticsManager() {
        // Initialize stats
    }
    
    // Called by SimulationManager every step
    public void updateStatistics(double currentAvgSpeed, int currentVehicleCount) {
        this.averageSpeed = currentAvgSpeed;
        this.totalVehicles = currentVehicleCount;
        // Update other stats...
    }

    // --- Getters ---
    public double getAverageSpeed() { return averageSpeed; }
    public int getTotalVehicles() { return totalVehicles; }
    public int getTotalTrafficlights() { return totalTrafficlights; }
    public double getAverageTravelTime() { return averageTravelTime; }
    public int getCongestionCount() { return congestionCount; }
}