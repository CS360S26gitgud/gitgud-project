package com.example.counsellingapp.model;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Arrays;

/**
 * Unit tests for shared identity fields via concrete subclasses.
 * User is abstract and cannot be instantiated directly.
 */
public class UserTest {
    @Test
    public void testStudentInitialization() {
        // Using Student concrete class to test base User fields
        Student student = new Student("abc-123", "Affan", "student@test.com");
        
        assertEquals("abc-123", student.getUid());
        assertEquals("Affan", student.getName());
        assertEquals("student@test.com", student.getEmail());
        assertEquals("student", student.getRole());
    }

    @Test
    public void testCounselorFields() {
        // Using Counselor concrete class to test specialization and availability
        Counselor counselor = new Counselor("xyz-999", "Dr. Moosa", "counselor@test.com", "Academic Advisor", Arrays.asList("Monday", "Wednesday"));

        assertEquals("xyz-999", counselor.getUid());
        assertEquals("Academic Advisor", counselor.getSpecialization());
        assertTrue(counselor.getAvailableDays().contains("Monday"));
        assertEquals("counselor", counselor.getRole());
    }
}


