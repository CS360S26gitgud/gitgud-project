package com.example.counsellingapp.model;

import com.google.firebase.Timestamp;

/**
 * Represents an anonymous student review of a completed counseling appointment.
 *
 * Firestore collection : "reviews"
 * Document ID strategy : appointmentId — enforces one review per appointment
 *                        at the database level and allows O(1) existence checks.
 *
 * Anonymity guarantee  : No studentId field is stored. The counselor can never
 *                        trace a review back to a specific student.
 */
public class Review {

    /**
     * Doubles as the Firestore document ID for this review.
     * Equals the appointmentId the review belongs to.
     */
    private String id;

    /**
     * UID of the counselor being reviewed.
     * Required so getReviewsForCounselor() can query by this field.
     */
    private String counselorId;

    /** Star rating provided by the student, between 1.0 and 5.0. */
    private float rating;

    /** Optional written comment. May be empty but never null after Firestore round-trip. */
    private String comment;

    /** Server-side timestamp of submission — used for display ordering. */
    private Timestamp timestamp;

    /** Required for Firestore automatic deserialization. */
    public Review() {}

    /**
     * @param id          The appointmentId (used as Firestore document ID).
     * @param counselorId UID of the counselor being reviewed.
     * @param rating      Star rating (1.0–5.0).
     * @param comment     Optional written feedback (pass "" if none).
     * @param timestamp   Submission time — use Timestamp.now() at call site.
     */
    public Review(String id, String counselorId, float rating, String comment, Timestamp timestamp) {
        this.id = id;
        this.counselorId = counselorId;
        this.rating = rating;
        this.comment = comment;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCounselorId() { return counselorId; }
    public void setCounselorId(String counselorId) { this.counselorId = counselorId; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}