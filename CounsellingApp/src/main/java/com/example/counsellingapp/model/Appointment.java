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

    //Persisted to Firestore (IDs only)
    private String id;
    private String studentId;
    private String counselorId;
    private String timeslotId;
    private String status;//states:"upcoming" | "cancelled" | "completed"
    
    // Haris's fields
    private String counselorName;
    private com.google.firebase.Timestamp dateTime;

    //Resolved at runtime by AppointmentController (never persists)
    @com.google.firebase.firestore.Exclude //Exclude makes sure firestore only works with IDs and not objects.
    private User student;

    @com.google.firebase.firestore.Exclude
    private User counselor;

    @com.google.firebase.firestore.Exclude
    private TimeSlot timeSlot;

    public Appointment() {}//empty constructor for Firestore deserialization

    public Appointment(String id, String studentId, String counselorId, String timeslotId, String status){
        this.id = id;
        this.studentId = studentId;
        this.counselorId = counselorId;
        this.timeslotId = timeslotId;
        this.status = status;
    }

    // --- Persisting field accessors ---

    public String getId(){
        return id;
    }
    public void setId(String id){
        this.id = id;
    }
    public String getStudentId(){
        return studentId;
    }
    public void setStudentId(String studentId){
        this.studentId = studentId;
    }
    public String getCounselorId(){
        return counselorId;
    }
    public void setCounselorId(String counselorId){
        this.counselorId = counselorId;
    }
    public String getTimeslotId(){
        return timeslotId;
    }
    public void setTimeslotId(String timeslotId){
        this.timeslotId = timeslotId;
    }
    public String getStatus(){
        return status;
    }
    public void setStatus(String status){
        this.status = status;
    }

    public String getCounselorName() { return counselorName; }
    public void setCounselorName(String counselorName) { this.counselorName = counselorName; }

    public com.google.firebase.Timestamp getDateTime() { return dateTime; }
    public void setDateTime(com.google.firebase.Timestamp dateTime) { this.dateTime = dateTime; }

    //Resolved object accessors (set by controller)
    /*Student BLOCK*/
    public User getStudent(){
        return student;
    }
    public void setStudent(User student){
        this.student = student;
    }
    /*Counselor BLOCK*/
    public User getCounselor(){
        return counselor;
    }
    public void setCounselor(User counselor){
        this.counselor = counselor;
    }
    /*Timeslot BLOCK*/
    public TimeSlot getTimeSlot(){
        return timeSlot;
    }
    public void setTimeSlot(TimeSlot timeSlot){
        this.timeSlot = timeSlot;
    }
}