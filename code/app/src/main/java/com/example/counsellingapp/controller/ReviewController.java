package com.example.counsellingapp.controller;

import com.example.counsellingapp.model.Review;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Controller for all review-related Firestore operations.
 *
 * Collection : "reviews"  (new — must be added to Firestore security rules)
 * Document ID: appointmentId  (one document per appointment, enforced by design)
 *
 * US-06 : submitReview()              — student submits a review for a completed appointment
 * US-07 : getReviewsForCounselor()    — student views all reviews for a chosen counselor
 * US-15 : getReviewsForCounselor()    — counselor views their own received feedback (same method)
 *         getReviewedAppointmentIds() — bulk existence check used by AppointmentHistoryActivity
 */
public class ReviewController {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COL_REVIEWS = "reviews";

    // -------------------------------------------------------------------------
    // Callback interfaces
    // -------------------------------------------------------------------------

    public interface SimpleCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface ReviewListCallback {
        void onSuccess(List<Review> reviews);
        void onFailure(Exception e);
    }

    public interface ReviewedIdsCallback {
        void onSuccess(Set<String> reviewedAppointmentIds);
        void onFailure(Exception e);
    }

    // -------------------------------------------------------------------------
    // US-06: Submit a review
    // -------------------------------------------------------------------------

    /**
     * Writes a review document to Firestore using the appointmentId as the document ID.
     * Because we use set() (not add()), a second call for the same appointment will
     * silently overwrite — but SubmitReviewActivity checks existence before showing
     * the form, so this path is unreachable under normal app flow.
     *
     * @param review Fully populated Review object. review.getId() must equal the appointmentId.
     * @param cb     Fires onSuccess() on write completion, onFailure() on error.
     */
    public void submitReview(Review review, SimpleCallback cb) {
        db.collection(COL_REVIEWS)
                .document(review.getId())   // documentId = appointmentId
                .set(review)
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onFailure);
    }

    // -------------------------------------------------------------------------
    // US-07 / US-15: Fetch all reviews for a counselor
    // -------------------------------------------------------------------------

    /**
     * Retrieves all reviews where counselorId matches, sorted newest-first in memory.
     * Sorting is done client-side to avoid requiring a composite Firestore index
     * (consistent with the pattern used in CounselorController.searchCounselors).
     *
     * @param counselorId UID of the counselor whose reviews are requested.
     * @param cb          Delivers the sorted list on success.
     */
    public void getReviewsForCounselor(String counselorId, ReviewListCallback cb) {
        db.collection(COL_REVIEWS)
                .whereEqualTo("counselorId", counselorId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Review> reviews = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Review r = doc.toObject(Review.class);
                        r.setId(doc.getId()); // restore document ID (= appointmentId)
                        reviews.add(r);
                    }
                    // Sort newest-first in memory — avoids composite index requirement
                    reviews.sort((a, b) -> {
                        if (a.getTimestamp() == null || b.getTimestamp() == null) return 0;
                        return b.getTimestamp().compareTo(a.getTimestamp());
                    });
                    cb.onSuccess(reviews);
                })
                .addOnFailureListener(cb::onFailure);
    }

    // -------------------------------------------------------------------------
    // Bulk existence check (used by AppointmentHistoryActivity)
    // -------------------------------------------------------------------------

    /**
     * Given a list of appointmentIds, returns the subset that already have a review.
     * Uses a single whereIn(FieldPath.documentId(), ...) query — one Firestore read
     * regardless of how many IDs are passed (Firestore limit: 30 items per whereIn).
     *
     * If the list is empty, returns an empty Set immediately without touching Firestore.
     *
     * @param appointmentIds IDs to check. Should be pre-filtered to completed appointments only.
     * @param cb             Delivers a Set<String> of already-reviewed appointmentIds.
     */
    public void getReviewedAppointmentIds(List<String> appointmentIds, ReviewedIdsCallback cb) {
        if (appointmentIds == null || appointmentIds.isEmpty()) {
            cb.onSuccess(new HashSet<>());
            return;
        }

        db.collection(COL_REVIEWS)
                .whereIn(FieldPath.documentId(), appointmentIds)
                .get()
                .addOnSuccessListener(snapshots -> {
                    Set<String> reviewed = new HashSet<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        reviewed.add(doc.getId());
                    }
                    cb.onSuccess(reviewed);
                })
                .addOnFailureListener(cb::onFailure);
    }
}