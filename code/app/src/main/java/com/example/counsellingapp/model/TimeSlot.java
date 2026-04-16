package com.example.counsellingapp.model;
/**
 * Represents a counselor's available date/time window.
 * Works as the 'availability' in Firestore collection.
 * CRC: Represent a specific date/time window; support recurring slot creation.
 * Collaborators: Counselor, Appointment
 */
public class TimeSlot {

    private String id;
    private String counselorId;
    private String date;//format: "YYYY-MM-DD"
    private String startTime;//format: "HH:mm"
    private String endTime;//format: "HH:mm"
    private boolean booked;

    @com.google.firebase.firestore.Exclude
    private String counselorName;

    public TimeSlot() {}//empty constructor for Firestore deserialization

    public TimeSlot(String id, String counselorId, String date, String startTime, String endTime) {
        this.id = id;
        this.counselorId = counselorId;
        this.date= date;
        this.startTime= startTime;
        this.endTime= endTime;
        this.booked= false;
    }

    public String getId(){
        return id;
    }
    public void setId(String id){
        this.id = id;
    }
    public String getCounselorId(){
        return counselorId;
    }
    public void setCounselorId(String counselorId){
        this.counselorId = counselorId;
    }
    public String getDate(){
        return date;
    }
    public void setDate(String date){
        this.date = date;
    }

    public String getStartTime(){
        return startTime;
    }
    public void setStartTime(String startTime){
        this.startTime = startTime;
    }

    public String getEndTime(){
        return endTime;
    }
    public void setEndTime(String endTime){
        this.endTime = endTime;
    }

    public boolean isBooked(){
        return booked;
    }
    public void setBooked(boolean b){
        this.booked = b;
    }

    public String getCounselorName() {
        return counselorName;
    }

    public void setCounselorName(String name) {
        this.counselorName = name;
    }
}