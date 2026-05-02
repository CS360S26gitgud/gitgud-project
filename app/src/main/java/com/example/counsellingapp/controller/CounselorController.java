package com.example.counsellingapp.controller;

import com.example.counsellingapp.model.Counselor;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for student-facing counselor search and retrieval.
 *
 * <p><b>US-19 / US-20 enforcement:</b> All methods in this controller filter results
 * to only include counselors who are both {@code approved == true} <em>and</em>
 * {@code suspended == false}. This means:
 * <ul>
 *   <li>Newly registered counselors (awaiting admin approval) are invisible to students.
 *   <li>Rating-suspended counselors are invisible to students until an admin clears
 *       the suspension via {@link AdminController#clearSuspension}.
 * </ul>
 *
 * <p>Filtering is applied in memory to avoid Firestore composite index requirements,
 * consistent with the pattern used across the codebase. A single collection fetch
 * is made and the approved+active subset is derived client-side.
 *
 * <p>For admin-facing unfiltered access (all states), use
 * {@link AdminController#getAllCounselors}.
 *
 * <p>CRC Responsibilities:
 * <ul>
 *   <li>Fetch approved, non-suspended counselors for student-facing search (US-10).
 *   <li>Filter results by specialization and/or available day (US-10).
 * </ul>
 *
 * CRC Collaborators: {@link Counselor}, {@link CounselorListCallback}
 */
public class CounselorController {

    private static final String COLLECTION = "counselors";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Callback interface for operations that return a list of {@link Counselor} objects.
     */
    public interface CounselorListCallback {
        /**
         * Called when the counselor list has been successfully retrieved and filtered.
         *
         * @param counselors The filtered list. Never {@code null}; may be empty.
         */
        void onSuccess(List<Counselor> counselors);

        /**
         * Called when the Firestore query fails.
         *
         * @param e The exception describing the failure.
         */
        void onFailure(Exception e);
    }

    /**
     * Returns whether a given counselor should be visible to students.
     * A counselor is visible only when {@code approved == true} AND
     * {@code suspended == false}.
     *
     * @param c The {@link Counselor} to evaluate.
     * @return {@code true} if the counselor should appear in student-facing lists.
     */
    private boolean isVisible(Counselor c) {
        return c.isApproved() && !c.isSuspended();
    }

    /**
     * Fetches all counselors that are approved and not suspended — the set visible
     * to students with no additional filtering.
     *
     * @param callback Delivers the filtered list of visible {@link Counselor} objects.
     */
    public void getAllCounselors(CounselorListCallback callback) {
        db.collection(COLLECTION)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Counselor> visible = snapshots.toObjects(Counselor.class)
                            .stream()
                            .filter(this::isVisible)
                            .collect(Collectors.toList());
                    callback.onSuccess(visible);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Fetches all visible counselors (approved and not suspended) and filters them
     * in memory by specialization and/or available day.
     *
     * <p>Both filter parameters are optional. Passing {@code null} or an empty string
     * skips that filter, behaving identically to {@link #getAllCounselors}.
     *
     * <p>Specialization matching is case-insensitive. The UI label {@code "General"}
     * matches counselors whose {@link Counselor#getSpecialization()} is {@code null}
     * or empty — that label is never stored in Firestore.
     *
     * <p>Day matching is case-insensitive against {@link Counselor#getAvailableDays()}.
     *
     * @param specialization Specialization filter, or {@code null} / empty to skip.
     *                       Pass {@code "General"} to match unspecialized counselors.
     * @param day            Full day name (e.g. {@code "Monday"}), or {@code null} to skip.
     * @param callback       Delivers the filtered list of visible {@link Counselor} objects.
     */
    public void searchCounselors(String specialization, String day,
                                 CounselorListCallback callback) {
        db.collection(COLLECTION)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Counselor> counselors = snapshots.toObjects(Counselor.class)
                            .stream()
                            .filter(this::isVisible)
                            .collect(Collectors.toList());

                    if (specialization != null && !specialization.isEmpty()) {
                        final String specLower = specialization.trim().toLowerCase();
                        counselors = counselors.stream()
                                .filter(c -> {
                                    if (specLower.equals("general")) {
                                        return c.getSpecialization() == null
                                                || c.getSpecialization().isEmpty();
                                    }
                                    return c.getSpecialization() != null
                                            && c.getSpecialization().trim()
                                            .toLowerCase().equals(specLower);
                                })
                                .collect(Collectors.toList());
                    }

                    if (day != null && !day.isEmpty()) {
                        final String dayLower = day.trim().toLowerCase();
                        counselors = counselors.stream()
                                .filter(c -> c.getAvailableDays() != null
                                        && c.getAvailableDays().stream()
                                        .anyMatch(d -> d.toLowerCase().equals(dayLower)))
                                .collect(Collectors.toList());
                    }

                    callback.onSuccess(counselors);
                })
                .addOnFailureListener(callback::onFailure);
    }
}