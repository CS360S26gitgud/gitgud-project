package com.example.counsellingapp.model;
/**
 * Represents a counselor's available date/time window.
 * Works as the 'availability' in Firestore collection.
 * CRC: Represent a specific date/time window; support recurring slot creation.
 * Collaborators: Counselor, Appointment
 */
public class TimeSlot {

    /** Unique identifier for the time slot record in Firestore. */
    private String id;

    /** The unique ID of the counselor who owns this availability window. */
    private String counselorId;

    /** The date of the availability, formatted as "YYYY-MM-DD". */
    private String date;//format: "YYYY-MM-DD"

    /** The start time of the slot, formatted as "HH:mm" (24-hour clock). */
    private String startTime;//format: "HH:mm"

    /** The end time of the slot, formatted as "HH:mm" (24-hour clock). */
    private String endTime;//format: "HH:mm"

    /** Flag indicating whether this specific slot has been reserved by a student. */
    private boolean booked;

    /** * The display name of the counselor. 
     * This is a transient field used for UI binding and is not persisted to Firestore. 
     */
    @com.google.firebase.firestore.Exclude
    private String counselorName;

    /**
     * Default constructor required for Firestore automatic deserialization.
     */
    public TimeSlot() {}//empty constructor for Firestore deserialization

    /**
     * Parameterized constructor for creating a new availability instance.
     *
     * @param id          Unique identifier for the time slot.
     * @param counselorId ID of the counselor providing the slot.
     * @param date        The scheduled date (YYYY-MM-DD).
     * @param startTime   The beginning of the window (HH:mm).
     * @param endTime     The end of the window (HH:mm).
     */
    public TimeSlot(String id, String counselorId, String date, String startTime, String endTime) {
        this.id = id;
        this.counselorId = counselorId;
        this.date= date;
        this.startTime= startTime;
        this.endTime= endTime;
        this.booked= false;
    }

    /** @return The unique Firestore document ID. */
    public String getId() { return id; }

    /** @param id The unique Firestore document ID to set. */
    public void setId(String id) { this.id = id; }

    /** @return The ID of the associated counselor. */
    public String getCounselorId() { return counselorId; }

    /** @param counselorId The ID of the associated counselor to set. */
    public void setCounselorId(String counselorId) { this.counselorId = counselorId; }

    /** @return The date string in "YYYY-MM-DD" format. */
    public String getDate() { return date; }

    /** @param date The date string to set (expected: "YYYY-MM-DD"). */
    public void setDate(String date) {this.date = date;}

    /** @return The start time string in "HH:mm" format. */
    public String getStartTime(){return startTime;}

    /** @param startTime The start time to set (expected: "HH:mm"). */
    public void setStartTime(String startTime){this.startTime = startTime;}

    /** @return The end time string in "HH:mm" format. */
    public String getEndTime(){return endTime;}

    /** @param endTime The end time to set (expected: "HH:mm"). */
    public void setEndTime(String endTime){this.endTime = endTime;}

    /** @return True if the slot is already booked, false otherwise. */
    public boolean isBooked(){return booked;}

    /** @param b The booking status to set. */
    public void setBooked(boolean b){this.booked = b;}

    /** * @return The counselor's display name. 
     * Note: This field is excluded from Firestore persistence.
     */
    public String getCounselorName() {return counselorName;}

    /** @param name The counselor's display name to set for transient use. */
    public void setCounselorName(String name) {this.counselorName = name;}

}

