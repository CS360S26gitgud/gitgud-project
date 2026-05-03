package com.example.counsellingapp.model;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the Student model (US-18).
 */
public class StudentTest {

    @Test
    public void testStudentInitialization() {
        Student student = new Student("s-1", "Alice", "alice@test.com");
        
        assertEquals("s-1", student.getUid());
        assertEquals("Alice", student.getName());
        assertTrue("Students should be active by default", student.isActive());
        assertEquals("student", student.getRole());
    }

    @Test
    public void testDeactivation() {
        Student student = new Student("s-2", "Bob", "bob@test.com");
        student.setActive(false);
        
        assertFalse("Student should be inactive after setActive(false)", student.isActive());
    }
}
