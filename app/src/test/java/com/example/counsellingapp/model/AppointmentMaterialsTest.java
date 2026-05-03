package com.example.counsellingapp.model;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for Appointment materials (US-17).
 */
public class AppointmentMaterialsTest {

    @Test
    public void testMaterialsInitialization() {
        Appointment appt = new Appointment();
        assertNotNull("Materials list should be initialized", appt.getMaterials());
        assertEquals(0, appt.getMaterials().size());
    }

    @Test
    public void testSetAndGetMaterials() {
        Appointment appt = new Appointment();
        List<String> materials = Arrays.asList("Link 1", "Note A");
        appt.setMaterials(materials);
        
        assertEquals(2, appt.getMaterials().size());
        assertEquals("Link 1", appt.getMaterials().get(0));
        assertEquals("Note A", appt.getMaterials().get(1));
    }
}
