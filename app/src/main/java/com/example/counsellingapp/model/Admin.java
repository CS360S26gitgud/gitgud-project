package com.example.counsellingapp.model;

import com.example.counsellingapp.controller.AdminController;
import com.example.counsellingapp.view.LandingActivity;

/**
 * Represents an administrator user in the counselling system.
 *
 * <p>Persisted to the Firestore {@code admins/} collection. Documents in this
 * collection are always deserialized as {@code Admin} via {@code toObject(Admin.class)}.
 *
 * <p>Admin accounts are created out-of-band (directly in the Firebase console or
 * via a secure setup script) and never through the public registration screen.
 * An admin's authority is encoded entirely by their presence in the {@code admins/}
 * collection — no permission flags are stored on the document itself.
 *
 * <p>{@link LandingActivity#routeByRole} checks the {@code admins/} collection first
 * during routing; a matching document routes the user to {@code AdminDashboardActivity}.
 *
 * <p>CRC Responsibilities:
 * <ul>
 *   <li>Represent an admin identity in Firestore and in memory.
 *   <li>Serve as the subject of admin-only operations in {@link AdminController}.
 * </ul>
 *
 * CRC Collaborators: {@link User}, {@link AdminController}
 */
public class Admin extends User {

    /**
     * No-arg constructor required by Firestore for automatic deserialization.
     */
    public Admin() {}

    /**
     * Parameterized constructor for creating a new admin instance.
     *
     * @param uid   The unique Firebase Authentication UID.
     * @param name  The administrator's full display name.
     * @param email The administrator's registered email address.
     */
    public Admin(String uid, String name, String email) {
        super(uid, name, email);
    }

    @Override
    public String getRole() {
        return "admin";
    }
}
