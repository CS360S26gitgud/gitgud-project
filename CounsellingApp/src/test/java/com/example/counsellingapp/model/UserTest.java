package com.example.counsellingapp.model;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Arrays;

public class UserTest {
    @Test
    public void testUserInitialization() {
        User student = new User("abc-123", "student", "student@lums.edu.pk", "student");
        
        assertEquals("abc-123", student.getUid());
        assertEquals("student@lums.edu.pk", student.getEmail());
        assertEquals("student", student.getRole());
    }

    @Test
    public void testCounselorFields() {
        User counselor = new User("xyz-999", "counselor", "counselor@lums.edu.pk", "counselor");
        counselor.setSpecialization("Academic Advisor");
        counselor.setAvailableDays(Arrays.asList("Monday", "Wednesday"));

        assertEquals("counselor", counselor.getRole());
        assertEquals("Academic Advisor", counselor.getSpecialization());
        assertTrue(counselor.getAvailableDays().contains("Monday"));
    }
}
