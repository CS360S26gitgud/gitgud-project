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

    // Required empty constructor for Firestore deserialization
    public User() {}

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