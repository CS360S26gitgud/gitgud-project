package com.example.counsellingapp.model;

import com.google.firebase.Timestamp;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the Review model (US-06).
 */
public class ReviewTest {

    @Test
    public void testReviewInitialization() {
        Timestamp now = Timestamp.now();
        Review review = new Review("appt-123", "counselor-456", 4.5f, "Great session!", now);

        assertEquals("appt-123", review.getId());
        assertEquals("counselor-456", review.getCounselorId());
        assertEquals(4.5f, review.getRating(), 0.01f);
        assertEquals("Great session!", review.getComment());
        assertEquals(now, review.getTimestamp());
    }

    @Test
    public void testReviewSetters() {
        Review review = new Review();
        review.setId("appt-999");
        review.setRating(3.0f);
        review.setComment("Average.");

        assertEquals("appt-999", review.getId());
        assertEquals(3.0f, review.getRating(), 0.1f);
        assertEquals("Average.", review.getComment());
    }
}
