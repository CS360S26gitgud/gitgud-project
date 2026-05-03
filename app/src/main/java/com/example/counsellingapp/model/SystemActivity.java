package com.example.counsellingapp.model;

import com.google.firebase.Timestamp;

/**
 * Represents a single system event for US-21 monitoring.
 * Tracks what happened, when, and who initiated it.
 */
public class SystemActivity {
    /** Unique ID of the activity document in Firestore. */
    private String id;
    /** Category of event (e.g., BOOKING, CANCELLATION, APPROVAL). */
    private String type;
    /** Human-readable description of the event. */
    private String description;
    /** Name of the user (or "System") who triggered the event. */
    private String initiatorName;
    /** Server-side timestamp when the event was logged. */
    private Timestamp timestamp;


    public SystemActivity() {} // Required for Firestore

    public SystemActivity(String type, String description, String initiatorName) {
        this.type = type;
        this.description = description;
        this.initiatorName = initiatorName;
        this.timestamp = Timestamp.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getInitiatorName() { return initiatorName; }
    public void setInitiatorName(String initiatorName) { this.initiatorName = initiatorName; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}

