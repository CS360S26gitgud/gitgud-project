package com.example.counsellingapp.model;

import com.example.counsellingapp.controller.AdminController;

/**
 * Represents a student user in the counselling system.
 *
 * <p>Persisted to the Firestore {@code students/} collection. Documents in this
 * collection are always deserialized as {@code Student} via {@code toObject(Student.class)}.
 *
 * <p><b>US-18:</b> The {@link #active} flag supports admin-controlled account
 * deactivation. A deactivated student ({@code active == false}) is blocked from logging
 * in — {@code AuthController.loginUser()} checks this flag after Firebase Auth succeeds
 * and rejects the session before the dashboard is reached.
 *
 * <p>Students have no API surface to reactivate themselves; only an admin acting
 * through {@link AdminController#setStudentActive} can set {@code active = true},
 * satisfying US-18 acceptance criterion 4.
 *
 * <p>CRC Responsibilities:
 * <ul>
 *   <li>Represent a student account in Firestore and in memory.
 *   <li>Carry the {@link #active} flag that governs login access (US-18).
 *   <li>Act as the typed value of {@link Appointment#getStudent()} after hydration.
 * </ul>
 *
 * CRC Collaborators: {@link User}, {@link Appointment}, {@link AdminController}
 */
public class Student extends User {

    /**
     * Whether this student account is currently active.
     *
     * <p>Defaults to {@code true} at creation. When set to {@code false} by an admin,
     * the student is blocked at the login step. Only an admin can restore it to
     * {@code true} — there is no student-facing reactivation path (US-18 criterion 4).
     */
    private boolean active = true;

    /**
     * No-arg constructor required by Firestore for automatic deserialization.
     */
    public Student() {}

    /**
     * Parameterized constructor for creating a new, active student account.
     *
     * @param uid   The unique Firebase Authentication UID.
     * @param name  The student's full display name.
     * @param email The student's registered email address.
     */
    public Student(String uid, String name, String email) {
        super(uid, name, email);
        this.active = true;
    }

    /**
     * Returns whether this student account is currently active.
     *
     * @return {@code true} if the account can log in; {@code false} if deactivated by an admin.
     */
    public boolean isActive() { return active; }

    /**
     * Sets the active state of this account.
     * Should only be called via {@link AdminController} — never by student-facing code.
     *
     * @param active {@code true} to activate; {@code false} to deactivate.
     */
    public void setActive(boolean active) { this.active = active; }
}