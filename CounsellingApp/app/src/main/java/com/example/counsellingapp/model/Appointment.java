package com.example.counsellingapp.model;

import com.google.firebase.Timestamp;

/**
 * Represents a counseling appointment.
 * Mirrors the 'appointments' Firestore collection.
 */
public class Appointment {
    private String id;
    private String studentId;
    private String counselorId;
    private String counselorName;
    private Timestamp dateTime;
    private String status; // e.g., "scheduled", "completed", "cancelled"

    public Appointment() {}

    public Appointment(String id, String studentId, String counselorId, String counselorName, Timestamp dateTime, String status) {
        this.id = id;
        this.studentId = studentId;
        this.counselorId = counselorId;
        this.counselorName = counselorName;
        this.dateTime = dateTime;
        this.status = status;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getCounselorId() { return counselorId; }
    public void setCounselorId(String counselorId) { this.counselorId = counselorId; }

    public String getCounselorName() { return counselorName; }
    public void setCounselorName(String counselorName) { this.counselorName = counselorName; }

    public Timestamp getDateTime() { return dateTime; }
    public void setDateTime(Timestamp dateTime) { this.dateTime = dateTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}