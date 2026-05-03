package com.example.counsellingapp.model;

/**
 * Abstract base class representing a registered system user.
 *
 * <p>Shared identity fields (uid, name, email) live here. Role-specific fields live
 * exclusively on the concrete subclasses: {@link Student}, {@link Counselor},
 * and {@link Admin}. There is no {@code role} field — the Firestore collection itself
 * encodes the type, making a runtime discriminator unnecessary during deserialization.
 *
 * <p>Firestore collections and their concrete types:
 * <ul>
 *   <li>{@code students/}   — deserialized as {@link Student}
 *   <li>{@code counselors/} — deserialized as {@link Counselor}
 *   <li>{@code admins/}     — deserialized as {@link Admin}
 * </ul>
 *
 * <p>Because Firestore requires a concrete class for {@code toObject()}, this class is
 * intentionally abstract. Each collection uses its own concrete type directly.
 *
 * <p>CRC Responsibilities:
 * <ul>
 *   <li>Hold shared identity data common to all user types.
 *   <li>Provide a stable base type for {@link Appointment} transient fields.
 *   <li>Expose a role discriminator for runtime logic.
 * </ul>
 *
 * CRC Collaborators: {@link Student}, {@link Counselor}, {@link Admin}, {@link Appointment}
 */
public abstract class User {

    /** Unique identifier provided by Firebase Authentication. */
    private String uid;

    /** Full display name of the user. */
    private String name;

    /** Primary email address associated with the account. */
    private String email;

    /**
     * No-arg constructor required by Firestore for deserialization of concrete subclasses.
     */
    public User() {}

    /**
     * Parameterized constructor for creating a new user instance.
     *
     * @param uid   The unique Firebase Authentication UID.
     * @param name  The full display name of the user.
     * @param email The user's registered email address.
     */
    public User(String uid, String name, String email) {
        this.uid   = uid;
        this.name  = name;
        this.email = email;
    }

    /**
     * Returns the role of the user (e.g., "student", "counselor", "admin").
     * Implementation is provided by concrete subclasses.
     *
     * @return The user's role string.
     */
    public abstract String getRole();

    /** @return The unique Firebase Authentication UID. */
    public String getUid() { return uid; }

    /** @param uid The Firebase UID to assign. */
    public void setUid(String uid) { this.uid = uid; }

    /** @return The user's full display name. */
    public String getName() { return name; }

    /** @param name The display name to assign. */
    public void setName(String name) { this.name = name; }

    /** @return The user's registered email address. */
    public String getEmail() { return email; }

    /** @param email The email address to assign. */
    public void setEmail(String email) { this.email = email; }
}