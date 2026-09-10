package com.emergency.service;

import com.emergency.exception.InvalidRequestException;
import com.emergency.model.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

public class DispatchService {
    private final Map<String, Ambulance> ambulances = new ConcurrentHashMap<>();
    private final PriorityBlockingQueue<EmergencyRequest> waitingQueue = new PriorityBlockingQueue<>();
    private final List<String> executionHistory = Collections.synchronizedList(new ArrayList<>());

    public void registerAmbulance(Ambulance ambulance) {
        if (ambulance == null || ambulance.getId() == null) {
            throw new InvalidRequestException("Invalid ambulance details.");
        }
        ambulances.put(ambulance.getId(), ambulance);
        processWaitingQueue(); // Re-evaluate whenever an engine configuration expands
    }

    public synchronized void submitEmergencyRequest(EmergencyRequest request) {
        if (request == null || request.getPatientId() == null) {
            throw new InvalidRequestException("Invalid emergency request schema data.");
        }
        
        Ambulance allocated = findBestAvailableAmbulance(request);
        if (allocated != null) {
            dispatchAmbulance(allocated, request);
        } else {
            waitingQueue.add(request);
            executionHistory.add("No available units found. Patient " + request.getPatientId() + " placed in priority waiting line.");
        }
    }

    private Ambulance findBestAvailableAmbulance(EmergencyRequest request) {
        Ambulance bestMatch = null;
        double shortestDistance = Double.MAX_VALUE;

        for (Ambulance ambulance : ambulances.values()) {
            if (ambulance.getState() == AmbulanceState.AVAILABLE && ambulance.getType() == request.getPreferredType()) {
                double distance = calculateDistance(ambulance.getCurrentLatitude(), ambulance.getCurrentLongitude(),
                        request.getPickupLatitude(), request.getPickupLongitude());
                if (distance  getWaitingQueue() { return waitingQueue; }
    public List<String> getExecutionHistory() { return executionHistory; }
}
