package com.example.counsellingapp.controller;

import com.example.counsellingapp.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Handles Firebase Auth and Firestore operations for login and registration.
 * Outstanding issue: admin approval flow not yet implemented.
 */
public class AuthController {

    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Creates a Firebase Auth account and writes the user document to Firestore.
     * Role is supplied by the caller — determined by the user's selection on
     * the registration screen.
     */

    public void registerUser(String name, String email,
                             String password, String role,
                             AuthCallback cb) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid  = result.getUser().getUid();
                    User   user = new User(uid, name, email, role);
                    db.collection("users").document(uid)
                            .set(user)
                            .addOnSuccessListener(v -> cb.onSuccess())
                            .addOnFailureListener(cb::onFailure);
                })
                .addOnFailureListener(cb::onFailure);
    }

    /**
     * Authenticates the user. Role resolution and dashboard routing are
     * handled by LandingActivity.routeByRole() after this succeeds.
     */
    public void loginUser(String email, String password, AuthCallback cb) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> cb.onSuccess())
                .addOnFailureListener(cb::onFailure);
    }
}
