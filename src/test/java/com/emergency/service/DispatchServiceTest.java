package com.emergency.service; 

import com.emergency.exception.InvalidRequestException;
import com.emergency.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test; 

import static org.junit.jupiter.api.Assertions.*; 

public class DispatchServiceTest {
private DispatchService dispatchService; 

@BeforeEach
public void setup() {
dispatchService = new DispatchService();
}

@Nested
@DisplayName("Positive Test Cases")
class PositiveTests {
@Test
@DisplayName("Should immediately dispatch the closest available matching ambulance")
public void testImmediateDispatchToClosestAmbulance() {
    // Register a far-away ICU ambulance (approx 11 km away)
    Ambulance farAmbulance = new Ambulance("AMB-FAR", AmbulanceType.ICU, "Far Driver", 13.02, 79.20);
    // Register a near ICU ambulance (approx 3 km away)
    Ambulance nearAmbulance = new Ambulance("AMB-NEAR", AmbulanceType.ICU, "Near Driver", 12.94, 79.14);
    
    dispatchService.registerAmbulance(farAmbulance);
    dispatchService.registerAmbulance(nearAmbulance);

    // Submit an ICU emergency request at location (12.92, 79.13)
    EmergencyRequest request = new EmergencyRequest("P-001", EmergencyPriority.CRITICAL, AmbulanceType.ICU, 12.92, 79.13, "City General");
    dispatchService.submitEmergencyRequest(request);

    // Assert that the near ambulance was selected and dispatched
    assertEquals(AmbulanceState.DISPATCHED, nearAmbulance.getState());
    assertEquals(AmbulanceState.AVAILABLE, farAmbulance.getState());
    
    // Verify execution logs captured the assignment
    boolean logContainsNearAmbulance = dispatchService.getExecutionHistory().stream()
            .anyMatch(log -> log.contains("AMB-NEAR") && log.contains("P-001"));
    assertTrue(logContainsNearAmbulance, "Execution history should log the correct ambulance allocation.");
}

@Test
@DisplayName("Should maintain waiting queue sorted by priority when resources are unavailable")
public void testPriorityQueueSortingAndAutoAllocation() {
    // Register an ICU ambulance and immediately set it to busy status
    Ambulance ambulance = new Ambulance("AMB-01", AmbulanceType.ICU, "John Doe", 12.92, 79.13);
    dispatchService.registerAmbulance(ambulance);
    dispatchService.updateAmbulanceState("AMB-01", AmbulanceState.DISPATCHED, 12.92, 79.13);

    // Submit a MODERATE priority request first, followed by a CRITICAL request
    EmergencyRequest moderateReq = new EmergencyRequest("P-MOD", EmergencyPriority.MODERATE, AmbulanceType.ICU, 12.95, 79.15, "City Hospital");
    EmergencyRequest criticalReq = new EmergencyRequest("P-CRIT", EmergencyPriority.CRITICAL, AmbulanceType.ICU, 12.96, 79.16, "City Hospital");

    dispatchService.submitEmergencyRequest(moderateReq);
    dispatchService.submitEmergencyRequest(criticalReq);

    // Assert that the CRITICAL request jumped to the front of the queue
    assertFalse(dispatchService.getWaitingQueue().isEmpty());
    assertEquals("P-CRIT", dispatchService.getWaitingQueue().peek().getPatientId(), "Critical requests must head the queue over moderate requests.");

    // Free up the ambulance to trigger the automatic queue allocation engine
    dispatchService.updateAmbulanceState("AMB-01", AmbulanceState.AVAILABLE, 12.92, 79.13);
    
    // Assert that the CRITICAL request was consumed and processed, leaving MODERATE at the top
    assertEquals("P-MOD", dispatchService.getWaitingQueue().peek().getPatientId());
    assertEquals(AmbulanceState.DISPATCHED, ambulance.getState());
}

@Test
@DisplayName("Should successfully cycle through the entire state pipeline lifecycle workflow")
public void testAmbulanceStateTransitions() {
    Ambulance ambulance = new Ambulance("AMB-02", AmbulanceType.BASIC, "Jane Smith", 12.0, 79.0);
    dispatchService.registerAmbulance(ambulance);

    // Cycle validation through state sequence rules explicitly required by system criteria
    dispatchService.updateAmbulanceState("AMB-02", AmbulanceState.DISPATCHED, 12.1, 79.1);
    assertEquals(AmbulanceState.DISPATCHED, ambulance.getState());

    dispatchService.updateAmbulanceState("AMB-02", AmbulanceState.EN_ROUTE, 12.2, 79.2);
    assertEquals(AmbulanceState.EN_ROUTE, ambulance.getState());

    dispatchService.updateAmbulanceState("AMB-02", AmbulanceState.PATIENT_PICKED_UP, 12.3, 79.3);
    assertEquals(AmbulanceState.PATIENT_PICKED_UP, ambulance.getState());

    dispatchService.updateAmbulanceState("AMB-02", AmbulanceState.HOSPITAL_ARRIVED, 12.4, 79.4);
    assertEquals(AmbulanceState.HOSPITAL_ARRIVED, ambulance.getState());

    dispatchService.updateAmbulanceState("AMB-02", AmbulanceState.AVAILABLE, 12.4, 79.4);
    assertEquals(AmbulanceState.AVAILABLE, ambulance.getState());
}

}

@Nested
@DisplayName("Negative Test Cases")
class NegativeTests {
@Test
@DisplayName("Should throw exception when attempting to submit a null emergency request schema")
public void testSubmitNullEmergencyRequest() {
    InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
        dispatchService.submitEmergencyRequest(null);
    });
    assertEquals("Invalid emergency request schema data.", exception.getMessage());
}

@Test
@DisplayName("Should throw exception when parsing an empty patient details initialization structure")
public void testSubmitRequestWithNullPatientId() {
    EmergencyRequest invalidRequest = new EmergencyRequest(null, EmergencyPriority.HIGH, AmbulanceType.BASIC, 12.0, 79.0, "Clinic A");
    
    InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
        dispatchService.submitEmergencyRequest(invalidRequest);
    });
    assertEquals("Invalid emergency request schema data.", exception.getMessage());
}

@Test
@DisplayName("Should throw exception when attempting to update status on a non-existent ambulance id")
public void testUpdateStateForUnknownAmbulance() {
    InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
        dispatchService.updateAmbulanceState("NON-EXISTENT-ID", AmbulanceState.AVAILABLE, 12.0, 79.0);
    });
    assertEquals("Ambulance identifier matches no system entity record.", exception.getMessage());
}

@Test
@DisplayName("Should throw exception when attempting to register a null ambulance entity configuration")
public void testRegisterNullAmbulance() {
    InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
        dispatchService.registerAmbulance(null);
    });
    assertEquals("Invalid ambulance details.", exception.getMessage());
}

@Test
@DisplayName("Should prevent an already dispatched ambulance from handling multiple emergencies simultaneously")
public void testPreventDoubleAssignment() {
    Ambulance ambulance = new Ambulance("AMB-BUSY", AmbulanceType.BASIC, "Driver X", 12.0, 79.0);
    dispatchService.registerAmbulance(ambulance);

    EmergencyRequest firstRequest = new EmergencyRequest("P-FIRST", EmergencyPriority.HIGH, AmbulanceType.BASIC, 12.01, 79.01, "Hospital A");
    EmergencyRequest secondRequest = new EmergencyRequest("P-SECOND", EmergencyPriority.CRITICAL, AmbulanceType.BASIC, 12.02, 79.02, "Hospital B");

    // Dispatching first request consumes the available unit capacity immediately
    dispatchService.submitEmergencyRequest(firstRequest);
    assertEquals(AmbulanceState.DISPATCHED, ambulance.getState());

    // Submitting the second request shouldn't re-assign the busy ambulance; it must be queued
    dispatchService.submitEmergencyRequest(secondRequest);
    
    assertEquals(1, dispatchService.getWaitingQueue().size(), "Second request must go to waiting queue instead of hijacking the busy asset.");
    assertEquals("P-SECOND", dispatchService.getWaitingQueue().peek().getPatientId());
}

}

}
