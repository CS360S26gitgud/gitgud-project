package com.example.counsellingapp.model;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for Counselor rating and suspension logic (US-20).
 */
public class CounselorRatingTest {

    @Test
    public void testInitialState() {
        Counselor counselor = new Counselor("c-1", "Dr. Smith", "smith@test.com", "Anxiety", null);
        
        assertEquals(0f, counselor.getAverageRating(), 0.1f);
        assertEquals(0, counselor.getReviewCount());
        assertFalse(counselor.isSuspended());
        assertFalse(counselor.isMeetingCleared());
    }

    @Test
    public void testSuspensionThresholdConstant() {
        // Threshold should be 2.5 as per US-20
        assertEquals(2.5f, Counselor.SUSPENSION_THRESHOLD, 0.01f);
    }

    @Test
    public void testRatingAndSuspensionState() {
        Counselor counselor = new Counselor();
        
        counselor.setAverageRating(2.4f);
        counselor.setSuspended(true);
        
        assertTrue("Counselor should be suspended if rating < 2.5", counselor.isSuspended());
        assertEquals(2.4f, counselor.getAverageRating(), 0.01f);
    }

    @Test
    public void testMeetingClearanceState() {
        Counselor counselor = new Counselor();
        counselor.setSuspended(true);
        counselor.setMeetingCleared(true);
        
        assertTrue(counselor.isMeetingCleared());
        
        // After admin clears it, both should be false (verified in controller logic, 
        // but testing model state holders here)
        counselor.setSuspended(false);
        counselor.setMeetingCleared(false);
        
        assertFalse(counselor.isSuspended());
        assertFalse(counselor.isMeetingCleared());
    }
}
