package com.example.counsellingapp.model;

/**
 * Represents a system user (student, counselor, or admin).
 * Mirrors the 'users' Firestore collection.
 * Outstanding issue: admin approval flow not yet implemented.
 */
public class User {

    /** Unique identifier provided by Firebase Authentication. */
    private String uid;

    /** Full name of the user. */
    private String name;

    /** Primary email address associated with the account. */
    private String email;

    /** The user's role within the system (e.g., "student", "counselor", "admin"). */
    private String role;
    
    /** * Area of expertise for the counselor (e.g., "Academic Stress", "Career").
     * Relevant only if {@link #role} is "counselor".
     */
    private String specialization;

    /** * A list of days the counselor is generally active (e.g., ["Monday", "Wednesday"]).
     * Relevant only if {@link #role} is "counselor".
     */
    private java.util.List<String> availableDays; // e.g., ["Monday", "Wednesday"]

    /**
     * Default constructor required for Firestore automatic deserialization.
     */
    public User() {}

    /** @return The area of expertise for the counselor. */
    public String getSpecialization() { return specialization; }

    /** @param specialization The specialization to set for this counselor. */
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    /** @return A list of days (Strings) when the counselor is available. */
    public java.util.List<String> getAvailableDays() { return availableDays; }

    /** @param availableDays The list of active working days to set. */
    public void setAvailableDays(java.util.List<String> availableDays) { this.availableDays = availableDays; }

    /**
     * Parameterized constructor for creating a new user instance.
     *
     * @param uid   The unique Firebase UID.
     * @param name  The full name of the user.
     * @param email The user's email address.
     * @param role  The assigned system role.
     */
    public User(String uid, String name, String email, String role) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    /** @return The unique Firebase Authentication UID. */
    public String getUid() { return uid; }

    /** @param uid The unique Firebase Authentication UID to set. */
    public void setUid(String uid) { this.uid = uid; }

    /** @return The user's full name. */
    public String getName() { return name; }

    /** @param name The user's full name to set. */
    public void setName(String name) { this.name = name; }

    /** @return The user's registered email. */
    public String getEmail() { return email; }

    /** @param email The user's registered email to set. */
    public void setEmail(String email) { this.email = email; }

    /** @return The system role (student/counselor/admin). */
    public String getRole() { return role; }

    /** @param role The system role to assign to this user. */
    public void setRole(String role) { this.role = role; }
}

