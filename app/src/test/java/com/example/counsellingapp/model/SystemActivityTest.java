package com.example.counsellingapp.model;

import com.google.firebase.Timestamp;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the SystemActivity model (US-21).
 */
public class SystemActivityTest {

    @Test
    public void testActivityInitialization() {
        // The constructor handles timestamp internally using Timestamp.now()
        SystemActivity activity = new SystemActivity("BOOKING", "Description", "Initiator");
        
        assertEquals("BOOKING", activity.getType());
        assertEquals("Description", activity.getDescription());
        assertEquals("Initiator", activity.getInitiatorName());
        assertNotNull("Timestamp should be auto-initialized", activity.getTimestamp());
    }

    @Test
    public void testActivitySetters() {
        SystemActivity activity = new SystemActivity();
        activity.setType("CANCELLATION");
        activity.setDescription("User cancelled");
        activity.setInitiatorName("System");
        
        assertEquals("CANCELLATION", activity.getType());
        assertEquals("User cancelled", activity.getDescription());
        assertEquals("System", activity.getInitiatorName());
    }
}

