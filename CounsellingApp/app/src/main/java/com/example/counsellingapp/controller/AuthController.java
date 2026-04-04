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

    public void registerStudent(String name, String email,
                                String password, AuthCallback cb) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    User user = new User(uid, name, email, "student");
                    db.collection("users").document(uid)
                            .set(user)
                            .addOnSuccessListener(v -> cb.onSuccess())
                            .addOnFailureListener(cb::onFailure);
                })
                .addOnFailureListener(cb::onFailure);
    }

    public void loginUser(String email, String password,
                          String expectedRole, AuthCallback cb) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    db.collection("users").document(uid).get()
                            .addOnSuccessListener(doc -> {
                                String role = doc.getString("role");
                                if (role != null && role.equals(expectedRole)) {
                                    cb.onSuccess();
                                } else {
                                    mAuth.signOut();
                                    cb.onFailure(new Exception("Unauthorized role"));
                                }
                            })
                            .addOnFailureListener(cb::onFailure);
                })
                .addOnFailureListener(cb::onFailure);
    }
}
