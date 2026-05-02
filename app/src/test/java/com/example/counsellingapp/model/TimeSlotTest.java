package com.example.counsellingapp.model;

import org.junit.Test;
import static org.junit.Assert.*;

public class TimeSlotTest {
    @Test
    public void testTimeSlotCreationAndDefaultUnbooked() {
        TimeSlot slot = new TimeSlot("slot-1", "counselor-1", "2026-03-10", "14:00", "15:00");
        
        assertEquals("slot-1", slot.getId());
        assertEquals("2026-03-10", slot.getDate());
        assertFalse("New slots should be unbooked by default", slot.isBooked());
    }

    @Test
    public void testBookingStatusAndNameChange() {
        TimeSlot slot = new TimeSlot("slot-2", "counselor-2", "2026-04-12", "09:00", "10:00");
        slot.setBooked(true);
        slot.setCounselorName("John Doe");

        assertTrue("Slot should now be marked as booked", slot.isBooked());
        assertEquals("John Doe", slot.getCounselorName());
    }
}
