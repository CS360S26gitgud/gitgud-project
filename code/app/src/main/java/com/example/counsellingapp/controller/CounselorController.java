package com.example.counsellingapp.controller;

import com.example.counsellingapp.model.User;
import com.google.firebase.firestore.FirebaseFirestore;

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
        // Always fetch all counselors and filter in memory.
        // This avoids composite index requirements and handles the "General"
        // display label which is never actually stored in Firestore.
        db.collection("users")
                .whereEqualTo("role", "counselor")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<User> counselors = queryDocumentSnapshots.toObjects(User.class);

                    // Filter by specialization in memory.
                    // "General" in the UI means null/empty in Firestore, so handle both.
                    if (specialization != null && !specialization.isEmpty()) {
                        final String specLower = specialization.trim().toLowerCase();
                        counselors = counselors.stream()
                                .filter(c -> {
                                    if (specLower.equals("general")) {
                                        // "General" matches counselors with no specialization set
                                        return c.getSpecialization() == null || c.getSpecialization().isEmpty();
                                    }
                                    return c.getSpecialization() != null &&
                                            c.getSpecialization().trim().toLowerCase().equals(specLower);
                                })
                                .collect(Collectors.toList());
                    }

                    // Filter by available day in memory — case-insensitive.
                    if (day != null && !day.isEmpty()) {
                        final String dayLower = day.trim().toLowerCase();
                        counselors = counselors.stream()
                                .filter(c -> c.getAvailableDays() != null &&
                                        c.getAvailableDays().stream()
                                                .anyMatch(d -> d.toLowerCase().equals(dayLower)))
                                .collect(Collectors.toList());
                    }

                    callback.onSuccess(counselors);
                })
                .addOnFailureListener(callback::onFailure);
    }
}