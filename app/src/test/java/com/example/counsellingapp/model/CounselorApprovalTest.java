package com.example.counsellingapp.model;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for Counselor approval logic (US-19).
 */
public class CounselorApprovalTest {

    @Test
    public void testApprovalInitialization() {
        Counselor counselor = new Counselor("c-2", "Dr. Jones", "jones@test.com", "Stress", null);
        assertFalse("New counselors should be unapproved by default", counselor.isApproved());
    }

    @Test
    public void testSetApproved() {
        Counselor counselor = new Counselor();
        counselor.setApproved(true);
        assertTrue(counselor.isApproved());
        
        counselor.setApproved(false);
        assertFalse(counselor.isApproved());
    }
}
