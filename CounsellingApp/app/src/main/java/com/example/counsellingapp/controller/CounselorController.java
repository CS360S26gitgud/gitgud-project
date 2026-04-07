package com.example.counsellingapp.controller;

import com.example.counsellingapp.model.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for managing counselor-related operations.
 * Handles US 10: Searching/Filtering counselors.
 */
public class CounselorController {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface CounselorListCallback {
        void onSuccess(List<User> counselors);
        void onFailure(Exception e);
    }

    public void getAllCounselors(CounselorListCallback callback) {
        db.collection("users")
                .whereEqualTo("role", "counselor")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<User> counselors = queryDocumentSnapshots.toObjects(User.class);
                    callback.onSuccess(counselors);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void searchCounselors(String specialization, String day, CounselorListCallback callback) {
        Query query = db.collection("users").whereEqualTo("role", "counselor");

        if (specialization != null && !specialization.isEmpty()) {
            query = query.whereEqualTo("specialization", specialization);
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<User> counselors = queryDocumentSnapshots.toObjects(User.class);
                    
                    // Firestore doesn't support 'array-contains' well with other equality filters in simple queries without indexes.
                    // For simplicity in this prototype, we'll filter the day in memory if needed.
                    if (day != null && !day.isEmpty()) {
                        counselors = counselors.stream()
                                .filter(c -> c.getAvailableDays() != null && c.getAvailableDays().contains(day))
                                .collect(Collectors.toList());
                    }
                    callback.onSuccess(counselors);
                })
                .addOnFailureListener(callback::onFailure);
    }
}