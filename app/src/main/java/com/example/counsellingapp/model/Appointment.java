package com.example.counsellingapp.model;

import java.util.ArrayList;
import java.util.List;


/**
 * Represents a booked session between a student and a counselor.
 * Mirrors the {@code appointments} Firestore collection.
 *
 * <p>Firestore stores only foreign-key IDs ({@link #studentId}, {@link #counselorId},
 * {@link #timeslotId}). Controllers resolve these into fully typed objects
 * ({@link #student}, {@link #counselor}, {@link #timeSlot}) before passing the
 * appointment to the View layer. Transient fields are marked {@code @Exclude} so
 * Firestore ignores them during serialization and deserialization.
 *
 * <p>The transient fields are now typed as {@link Student} and {@link Counselor}
 * rather than the former untyped {@code User}. This means incorrect hydration is
 * detectable at compile time, directly addressing the prior NPE risk (bug #1) where
 * {@code getStudent().getName()} could crash if hydration was incomplete.
 *
 * <p>Valid status values: {@code "upcoming"}, {@code "cancelled"}, {@code "completed"},
 * {@code "rescheduled"}.
 *
 * <p>CRC Responsibilities:
 * <ul>
 *   <li>Associate a student with a counselor and a time slot.
 *   <li>Track the lifecycle status of the session.
 *   <li>Carry resolved collaborator objects for View-layer binding.
 * </ul>
 *
 * CRC Collaborators: {@link Student}, {@link Counselor}, {@link TimeSlot}
 */
public class Appointment {

    /** Unique identifier for this appointment record in Firestore. */
    private String id;

    /**
     * Foreign key: the UID of the student participant.
     * Resolved at runtime into the {@link #student} transient field.
     */
    private String studentId;

    /**
     * Foreign key: the UID of the counselor participant.
     * Resolved at runtime into the {@link #counselor} transient field.
     */
    private String counselorId;

    /**
     * Foreign key: the Firestore document ID of the selected {@link TimeSlot}.
     * Resolved at runtime into the {@link #timeSlot} transient field.
     */
    private String timeslotId;

    /**
     * Current lifecycle state of this session.
     * Valid values: {@code "upcoming"}, {@code "cancelled"}, {@code "completed"},
     * {@code "rescheduled"}.
     */
    private String status;

    /**
     * Denormalized display name of the counselor, written at booking time.
     * Allows the student history screen to show the counselor's name without
     * resolving the {@code counselors/} document on every list load.
     */
    private String counselorName;
    /** List of resource links or notes attached by the counselor for this session. */
    private List<String> materials = new ArrayList<>();


    /** Precise date and time of the scheduled session as a Firebase Timestamp. */
    private com.google.firebase.Timestamp dateTime;

    /**
     * The resolved {@link Student} who booked this appointment.
     * Populated by the controller after fetching from {@code students/}.
     * Not persisted to Firestore.
     */
    @com.google.firebase.firestore.Exclude
    private Student student;

    /**
     * The resolved {@link Counselor} assigned to this appointment.
     * Populated by the controller after fetching from {@code counselors/}.
     * Not persisted to Firestore.
     */
    @com.google.firebase.firestore.Exclude
    private Counselor counselor;

    /**
     * The resolved {@link TimeSlot} for this appointment.
     * Populated by the controller after fetching from {@code availability/}.
     * Not persisted to Firestore.
     */
    @com.google.firebase.firestore.Exclude
    private TimeSlot timeSlot;

    /**
     * No-arg constructor required by Firestore for automatic deserialization.
     */
    public Appointment() {}

    /**
     * Parameterized constructor for creating a new appointment record.
     *
     * @param id          Unique Firestore document ID.
     * @param studentId   UID of the student participant.
     * @param counselorId UID of the counselor participant.
     * @param timeslotId  Firestore document ID of the selected {@link TimeSlot}.
     * @param status      Initial status; typically {@code "upcoming"} at creation.
     */
    public Appointment(String id, String studentId, String counselorId,
                       String timeslotId, String status) {
        this.id           = id;
        this.studentId    = studentId;
        this.counselorId  = counselorId;
        this.timeslotId   = timeslotId;
        this.status       = status;
    }

    /** @return The unique Firestore document ID for this appointment. */
    public String getId() { return id; }

    /** @param id The Firestore document ID to assign. */
    public void setId(String id) { this.id = id; }

    /** @return The UID of the student participant. */
    public String getStudentId() { return studentId; }

    /** @param studentId The student's Firebase UID to assign. */
    public void setStudentId(String studentId) { this.studentId = studentId; }

    /** @return The UID of the counselor participant. */
    public String getCounselorId() { return counselorId; }

    /** @param counselorId The counselor's Firebase UID to assign. */
    public void setCounselorId(String counselorId) { this.counselorId = counselorId; }

    /** @return The Firestore document ID of the selected {@link TimeSlot}. */
    public String getTimeslotId() { return timeslotId; }

    /** @param timeslotId The time slot ID to assign. */
    public void setTimeslotId(String timeslotId) { this.timeslotId = timeslotId; }

    /**
     * Returns the current lifecycle status of this appointment.
     *
     * @return One of {@code "upcoming"}, {@code "cancelled"}, {@code "completed"},
     *         or {@code "rescheduled"}.
     */
    public String getStatus() { return status; }

    /** @param status The new status value to assign. */
    public void setStatus(String status) { this.status = status; }

    /** @return The denormalized counselor display name stored at booking time. */
    public String getCounselorName() { return counselorName; }

    /** @param counselorName The denormalized counselor name to store. */
    public void setCounselorName(String counselorName) { this.counselorName = counselorName; }

    /** @return The Firebase Timestamp of the scheduled session. */
    public com.google.firebase.Timestamp getDateTime() { return dateTime; }

    /** @param dateTime The session timestamp to assign. */
    public void setDateTime(com.google.firebase.Timestamp dateTime) { this.dateTime = dateTime; }
    /** @return The list of resource links or notes attached by the counselor. */
    public List<String> getMaterials() { return materials; }

    /** @param materials The updated list of resources to attach to this appointment. */
    public void setMaterials(List<String> materials) { this.materials = materials; }



    /**
     * Returns the resolved {@link Student} for this appointment.
     * May be {@code null} if the controller has not yet hydrated this field.
     *
     * @return The resolved {@link Student}, or {@code null} if not yet hydrated.
     */
    public Student getStudent() { return student; }

    /**
     * Sets the resolved {@link Student}. Called by the controller after fetching
     * the student document. Not persisted to Firestore.
     *
     * @param student The resolved {@link Student} to assign.
     */
    public void setStudent(Student student) { this.student = student; }

    /**
     * Returns the resolved {@link Counselor} for this appointment.
     * May be {@code null} if the controller has not yet hydrated this field.
     *
     * @return The resolved {@link Counselor}, or {@code null} if not yet hydrated.
     */
    public Counselor getCounselor() { return counselor; }

    /**
     * Sets the resolved {@link Counselor}. Called by the controller after fetching
     * the counselor document. Not persisted to Firestore.
     *
     * @param counselor The resolved {@link Counselor} to assign.
     */
    public void setCounselor(Counselor counselor) { this.counselor = counselor; }

    /**
     * Returns the resolved {@link TimeSlot} for this appointment.
     * May be {@code null} if the controller has not yet hydrated this field.
     *
     * @return The resolved {@link TimeSlot}, or {@code null} if not yet hydrated.
     */
    public TimeSlot getTimeSlot() { return timeSlot; }

    /**
     * Sets the resolved {@link TimeSlot}. Called by the controller after fetching
     * the slot document. Not persisted to Firestore.
     *
     * @param timeSlot The resolved {@link TimeSlot} to assign.
     */
    public void setTimeSlot(TimeSlot timeSlot) { this.timeSlot = timeSlot; }
}