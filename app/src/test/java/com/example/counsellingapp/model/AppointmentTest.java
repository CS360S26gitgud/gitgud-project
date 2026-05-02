package com.example.counsellingapp.model;

import org.junit.Test;
import static org.junit.Assert.*;

public class AppointmentTest {
    @Test
    public void testAppointmentAttributes() {
        Appointment appt = new Appointment("appt-1", "student-1", "counselor-1", "slot-1", "upcoming");
        appt.setCounselorName("Jane Smith");
        
        assertEquals("appt-1", appt.getId());
        assertEquals("student-1", appt.getStudentId());
        assertEquals("upcoming", appt.getStatus());
        assertEquals("Jane Smith", appt.getCounselorName());
    }
}
