package com.emergency.service;

import com.emergency.exception.InvalidRequestException;
import com.emergency.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DispatchServiceTest {
    private DispatchService dispatchService;

    @BeforeEach
    public void setup() {
        dispatchService = new DispatchService();
    }

    @Test
    public void testPriorityQueueSortingAndAutoAllocation() {
        // Registering one single ICU ambulance 
        Ambulance ambulance = new Ambulance("AMB01", AmbulanceType.ICU, "John Doe", 12.92, 79.13);
        dispatchService.registerAmbulance(ambulance);

        // Submitting moderate issue first
        EmergencyRequest lowPriorityReq = new EmergencyRequest("P-MOD", EmergencyPriority.MODERATE, AmbulanceType.ICU, 12.95, 79.15, "City Cross Hospital");
        // Submitting critical issue second
        EmergencyRequest highPriorityReq = new EmergencyRequest("P-CRIT", EmergencyPriority.CRITICAL, AmbulanceType.ICU, 12.96, 79.16, "City Cross Hospital");

        // Set ambulance busy so both stay waiting
        dispatchService.updateAmbulanceState("AMB01", AmbulanceState.DISPATCHED, 12.92, 79.13);

        dispatchService.submitEmergencyRequest(lowPriorityReq);
        dispatchService.submitEmergencyRequest(highPriorityReq);

        // Verify CRITICAL jumps to top position
        assertEquals("P-CRIT", dispatchService.getWaitingQueue().peek().getPatientId());

        // Make ambulance available again to trigger automated consumption mechanics
        dispatchService.updateAmbulanceState("AMB01", AmbulanceState.AVAILABLE, 12.92, 79.13);
        
        // Critical is assigned, Moderate is now top of waiting queue
        assertEquals("P-MOD", dispatchService.getWaitingQueue().peek().getPatientId());
    }

    @Test
    public void testExceptionOnInvalidData() {
        assertThrows(InvalidRequestException.class, () -> {
            dispatchService.submitEmergencyRequest(null);
        });
    }
}
