package com.example.counsellingapp.model;

/**
 * Represents a system user (student, counselor, or admin).
 * Mirrors the 'users' Firestore collection.
 * Outstanding issue: admin approval flow not yet implemented.
 */
public class User {

    private String uid;
    private String name;
    private String email;
    private String role;
    
    // Counselor specific fields
    private String specialization;
    private java.util.List<String> availableDays; // e.g., ["Monday", "Wednesday"]

    // Required empty constructor for Firestore deserialization
    public User() {}

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public java.util.List<String> getAvailableDays() { return availableDays; }
    public void setAvailableDays(java.util.List<String> availableDays) { this.availableDays = availableDays; }

    public User(String uid, String name, String email, String role) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}