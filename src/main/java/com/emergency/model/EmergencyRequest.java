package com.emergency.model;

public class EmergencyRequest implements Comparable<EmergencyRequest> {
    private final String patientId;
    private final EmergencyPriority priority;
    private final AmbulanceType preferredType;
    private final double pickupLatitude;
    private final double pickupLongitude;
    private final String destinationHospital;
    private final long timestamp;

    public EmergencyRequest(String patientId, EmergencyPriority priority, AmbulanceType preferredType, 
                            double pickupLat, double pickupLon, String destinationHospital) {
        this.patientId = patientId;
        this.priority = priority;
        this.preferredType = preferredType;
        this.pickupLatitude = pickupLat;
        this.pickupLongitude = pickupLon;
        this.destinationHospital = destinationHospital;
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public int compareTo(EmergencyRequest other) {
        int priorityCompare = Integer.compare(this.priority.getRank(), other.priority.getRank());
        if (priorityCompare != 0) return priorityCompare;
        return Long.compare(this.timestamp, other.timestamp); // FIFO fallback for same priorities
    }

    // Getters
    public String getPatientId() { return patientId; }
    public EmergencyPriority getPriority() { return priority; }
    public AmbulanceType getPreferredType() { return preferredType; }
    public double getPickupLatitude() { return pickupLatitude; }
    public double getPickupLongitude() { return pickupLongitude; }
    public String getDestinationHospital() { return destinationHospital; }
}
