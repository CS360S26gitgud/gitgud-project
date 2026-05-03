package com.example.counsellingapp.model;

import com.example.counsellingapp.controller.AdminController;

import java.util.List;

/**
 * Represents a counselor user in the counselling system.
 *
 * <p>Persisted to the Firestore {@code counselors/} collection. Documents in this
 * collection are always deserialized as {@code Counselor} via {@code toObject(Counselor.class)}.
 *
 * <p><b>US-19:</b> The {@link #approved} flag controls whether a counselor appears to
 * students. New counselors registered by an admin start as unapproved ({@code approved == false})
 * until an admin explicitly approves them via {@link AdminController#setCounselorApproved}.
 * {@code CounselorController} filters out unapproved counselors from all student-facing queries,
 * satisfying the requirement that only authorized counselors appear to students.
 *
 * <p><b>US-20:</b> The system maintains a running average rating via {@link #averageRating}
 * and {@link #reviewCount}, updated incrementally by {@code ReviewController.submitReview()}
 * each time a student submits a review. The scale is 1.0–5.0 in 0.5-star increments.
 * If the computed average falls below {@link #SUSPENSION_THRESHOLD} (2.5 stars), the counselor
 * is automatically suspended ({@code suspended = true}) and hidden from all student-facing
 * queries. The suspension is lifted only when an admin calls
 * {@link AdminController#clearCounselorSuspension}, which sets {@code suspended = false}.
 * The {@link #meetingCleared} flag serves as the admin's acknowledgement that the required
 * meeting has been held; it is reset to {@code false} in the same write to keep the state
 * clean for any future suspension cycle.
 *
 * <p>CRC Responsibilities:
 * <ul>
 *   <li>Represent a counselor account in Firestore and in memory.
 *   <li>Carry specialization and availability data for search and filtering (US-10).
 *   <li>Track approval status for admin gating (US-19).
 *   <li>Track rating data and suspension state for threshold enforcement (US-20).
 *   <li>Act as the typed value of {@link Appointment#getCounselor()} after hydration.
 * </ul>
 *
 * CRC Collaborators: {@link User}, {@link Appointment}, {@link TimeSlot}, {@link AdminController}
 */
public class Counselor extends User {

    /**
     * The minimum average star rating a counselor must maintain before being
     * automatically suspended. Expressed on the 1.0–5.0 scale with 0.5 differentials.
     * Referenced by {@code ReviewController.updateCounselorRating()} for threshold checks.
     */
    public static final float SUSPENSION_THRESHOLD = 2.5f;

    // -------------------------------------------------------------------------
    // Specialization and availability (US-10)
    // -------------------------------------------------------------------------

    /**
     * Area of professional expertise (e.g. "Academic Stress", "Career Guidance").
     * May be {@code null} or empty for general-practice counselors; the UI displays
     * "General" in that case but never writes that string to Firestore.
     */
    private String specialization;

    /**
     * Days of the week on which the counselor has at least one available slot,
     * stored as full English day names (e.g. {@code ["Monday", "Wednesday"]}).
     * Maintained automatically by {@code AvailabilityController.addSlot()} via
     * {@code FieldValue.arrayUnion}.
     */
    private List<String> availableDays;

    // -------------------------------------------------------------------------
    // Approval (US-19)
    // -------------------------------------------------------------------------

    /**
     * Whether this counselor has been approved by an admin.
     * Defaults to {@code false} at registration. Only an admin calling
     * {@link AdminController#setCounselorApproved} can change this.
     * Unapproved counselors are excluded from all student-facing queries.
     */
    private boolean approved = false;

    // -------------------------------------------------------------------------
    // Rating and suspension (US-20)
    // -------------------------------------------------------------------------

    /**
     * Running average star rating across all submitted reviews (1.0–5.0 scale,
     * rounded to the nearest 0.5). Initialized to {@code 0f} (no reviews yet).
     * Updated incrementally by {@code ReviewController.submitReview()}.
     */
    private float averageRating = 0f;

    /**
     * Total number of reviews contributing to {@link #averageRating}.
     * Used for incremental average calculation:
     * {@code newAvg = (oldAvg * oldCount + newRating) / (oldCount + 1)}.
     */
    private int reviewCount = 0;

    /**
     * Whether this counselor has been automatically suspended because their
     * {@link #averageRating} fell below {@link #SUSPENSION_THRESHOLD}.
     * Set by {@code ReviewController}; cleared only by
     * {@link AdminController#clearCounselorSuspension} after the admin meeting.
     * Suspended counselors are hidden from all student-facing queries.
     */
    private boolean suspended = false;

    /**
     * Transient admin acknowledgement flag, set to {@code true} by
     * {@link AdminController#clearCounselorSuspension} to signal the post-suspension
     * meeting has been held. Immediately reset to {@code false} in the same atomic
     * write alongside {@link #suspended}, keeping the state clean for future cycles.
     */
    private boolean meetingCleared = false;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * No-arg constructor required by Firestore for automatic deserialization.
     */
    public Counselor() {}

    /**
     * Parameterized constructor for admin-initiated counselor registration.
     * The account starts unapproved, unsuspended, and with no rating data.
     *
     * @param uid            The unique Firebase Authentication UID.
     * @param name           The counselor's full display name.
     * @param email          The counselor's registered email address.
     * @param specialization Area of expertise, or {@code null} for general practice.
     * @param availableDays  Initial available days list, or {@code null} if not yet set.
     */
    public Counselor(String uid, String name, String email,
                     String specialization, List<String> availableDays) {
        super(uid, name, email);
        this.specialization = specialization;
        this.availableDays  = availableDays;
        this.approved       = false;
        this.suspended      = false;
        this.meetingCleared = false;
        this.averageRating  = 0f;
        this.reviewCount    = 0;
    }

    @Override
    public String getRole() {
        return "counselor";
    }

    // -------------------------------------------------------------------------
    // Specialization / availability accessors
    // -------------------------------------------------------------------------

    /** @return The counselor's area of expertise, or {@code null} if general practice. */
    public String getSpecialization() { return specialization; }

    /** @param specialization The specialization to assign; {@code null} for general practice. */
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    /** @return List of full English day names on which the counselor has available slots. */
    public List<String> getAvailableDays() { return availableDays; }

    /** @param availableDays The day list to assign. Maintained by {@code AvailabilityController}. */
    public void setAvailableDays(List<String> availableDays) { this.availableDays = availableDays; }

    // -------------------------------------------------------------------------
    // Approval accessors (US-19)
    // -------------------------------------------------------------------------

    /**
     * Returns whether this counselor has been approved by an admin.
     * Only approved counselors appear in student-facing search results.
     *
     * @return {@code true} if approved; {@code false} if pending admin approval.
     */
    public boolean isApproved() { return approved; }

    /**
     * Sets the admin approval state. Should only be called via {@link AdminController}.
     *
     * @param approved {@code true} to approve; {@code false} to revoke.
     */
    public void setApproved(boolean approved) { this.approved = approved; }

    // -------------------------------------------------------------------------
    // Rating and suspension accessors (US-20)
    // -------------------------------------------------------------------------

    /**
     * Returns the current average star rating (1.0–5.0, rounded to nearest 0.5).
     * Returns {@code 0f} if no reviews have been submitted yet.
     *
     * @return The average rating, or {@code 0f} if unrated.
     */
    public float getAverageRating() { return averageRating; }

    /** @param averageRating The new average rating value. Managed by {@code ReviewController}. */
    public void setAverageRating(float averageRating) { this.averageRating = averageRating; }

    /** @return Total number of reviews contributing to {@link #averageRating}. */
    public int getReviewCount() { return reviewCount; }

    /** @param reviewCount The new review count. Managed by {@code ReviewController}. */
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    /**
     * Returns whether this counselor is suspended due to a low average rating.
     * Suspended counselors are hidden from students regardless of approval state.
     *
     * @return {@code true} if suspended; {@code false} otherwise.
     */
    public boolean isSuspended() { return suspended; }

    /**
     * Sets the suspension state. Automatically set by {@code ReviewController} when
     * the average falls below {@link #SUSPENSION_THRESHOLD}; cleared by
     * {@link AdminController#clearCounselorSuspension}.
     *
     * @param suspended {@code true} to suspend; {@code false} to unsuspend.
     */
    public void setSuspended(boolean suspended) { this.suspended = suspended; }

    /**
     * Returns whether the admin has completed the post-suspension meeting.
     * This flag is reset to {@code false} immediately after the clearance write.
     *
     * @return {@code true} if the meeting has been cleared; {@code false} otherwise.
     */
    public boolean isMeetingCleared() { return meetingCleared; }

    /** @param meetingCleared {@code true} to signal meeting complete; reset to {@code false} after use. */
    public void setMeetingCleared(boolean meetingCleared) { this.meetingCleared = meetingCleared; }
}