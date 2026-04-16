package com.example.counsellingapp.model;
/**
 * Represents a booked session between a student and a counselor.
 * Works as the 'appointments' in Firestore collection.
 *
 * CRC Responsibilities:
 *   - Associate with a student and a counselor
 *   - Support cancellation and rescheduling
 *   - Store attached materials
 *   - Provide data for appointment history
 *
 * CRC Collaborators: Student, Counselor, TimeSlot, NotificationService, Material
 *
 * Firestore stores only the IDs (foreign keys).
 * The controller resolves them into full objects before handing to the View.
 * Transient fields are marked with @Exclude so Firestore ignores them.
 */
public class Appointment {

    /** Unique identifier for the appointment record in Firestore. */
    private String id;

    /** Foreign key representing the unique ID of the student. */
    private String studentId;

    /** Foreign key representing the unique ID of the counselor. */
    private String counselorId;

    /** Foreign key representing the unique ID of the selected time slot. */
    private String timeslotId;

    /** * Current state of the session.
     * Expected values: "upcoming", "cancelled", or "completed".
     */
    private String status;//states:"upcoming" | "cancelled" | "completed"

    /** Display name of the counselor associated with this session. */
    private String counselorName;

    /** Precise date and time for the scheduled session. */
    private com.google.firebase.Timestamp dateTime;

    /** * The resolved Student User object.
     * Resolved at runtime; not persisted to Firestore.
     */
    @com.google.firebase.firestore.Exclude
    private User student;

    /** * The resolved Counselor User object.
     * Resolved at runtime; not persisted to Firestore.
     */
    @com.google.firebase.firestore.Exclude
    private User counselor;

    /** * The resolved TimeSlot object.
     * Resolved at runtime; not persisted to Firestore.
     */
    @com.google.firebase.firestore.Exclude
    private TimeSlot timeSlot;

    /**
     * Default constructor required for Firestore automatic deserialization.
     */
    public Appointment() {}

    /**
     * Parameterized constructor for creating new appointment instances.
     *
     * @param id          Unique identifier for the appointment
     * @param studentId   ID of the student attending
     * @param counselorId ID of the assigned counselor
     * @param timeslotId  ID of the chosen time window
     * @param status      Initial status (usually "upcoming")
     */
    public Appointment(String id, String studentId, String counselorId, String timeslotId, String status){
        this.id = id;
        this.studentId = studentId;
        this.counselorId = counselorId;
        this.timeslotId = timeslotId;
        this.status = status;
    }

    // --- Persisting field accessors ---

    /** @return The unique Firestore document ID for this appointment. */
    public String getId(){
        return id;
    }

    /** @param id The unique Firestore document ID to set. */
    public void setId(String id){
        this.id = id;
    }

    /** @return The ID of the student participant. */
    public String getStudentId(){
        return studentId;
    }

    /** @param studentId The ID of the student participant to set. */
    public void setStudentId(String studentId){
        this.studentId = studentId;
    }

    /** @return The ID of the counselor participant. */
    public String getCounselorId(){
        return counselorId;
    }

    /** @param counselorId The ID of the counselor participant to set. */
    public void setCounselorId(String counselorId){
        this.counselorId = counselorId;
    }

    /** @return The ID of the selected time slot. */
    public String getTimeslotId(){
        return timeslotId;
    }

    /** @param timeslotId The ID of the selected time slot to set. */
    public void setTimeslotId(String timeslotId){
        this.timeslotId = timeslotId;
    }

    /** @return The current status of the appointment. */
    public String getStatus(){
        return status;
    }

    /** @param status The new status to set. */
    public void setStatus(String status){
        this.status = status;
    }

    /** @return The counselor's display name. */
    public String getCounselorName() { return counselorName; }

    /** @param counselorName The counselor's display name to set. */
    public void setCounselorName(String counselorName) { this.counselorName = counselorName; }

    /** @return The Firebase Timestamp of the appointment. */
    public com.google.firebase.Timestamp getDateTime() { return dateTime; }

    /** @param dateTime The Firebase Timestamp to set for this appointment. */
    public void setDateTime(com.google.firebase.Timestamp dateTime) { this.dateTime = dateTime; }

    /** @return The resolved Student User object (not persisted) */
    public User getStudent(){
        return student;
    }

    /** @param student The resolved Student User object (not persisted) */
    public void setStudent(User student){
        this.student = student;
    }

    /** @return The resolved Counselor User object (not persisted) */
    public User getCounselor(){
        return counselor;
    }

    /** @param counselor The resolved Counselor User object (not persisted) */
    public void setCounselor(User counselor){
        this.counselor = counselor;
    }

    /** @return The resolved TimeSlot object (not persisted) */
    public TimeSlot getTimeSlot(){
        return timeSlot;
    }

    /** @param timeSlot The resolved TimeSlot object (not persisted) */
    public void setTimeSlot(TimeSlot timeSlot){
        this.timeSlot = timeSlot;
    }
}