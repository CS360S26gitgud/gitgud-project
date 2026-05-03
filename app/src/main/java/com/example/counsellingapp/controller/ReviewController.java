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
    private final ActivityController activityController = new ActivityController();
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
     * Writes a review document to Firestore and atomically updates the counselor's
     * average rating and suspension state (US-20).
     *
     * <p>Uses a transaction to ensure that the incremental average calculation
     * and the suspension threshold check (2.5 stars) are performed atomically.
     * If the new average falls below the threshold, the counselor is marked
     * {@code suspended = true} immediately.
     *
     * @param review Fully populated Review object.
     * @param cb     Fires onSuccess() on write completion, onFailure() on error.
     */
    public void submitReview(Review review, SimpleCallback cb) {
        com.google.firebase.firestore.DocumentReference reviewRef =
                db.collection(COL_REVIEWS).document(review.getId());
        com.google.firebase.firestore.DocumentReference counselorRef =
                db.collection("counselors").document(review.getCounselorId());

        db.runTransaction(transaction -> {
            // 1. Write the review
            transaction.set(reviewRef, review);

            // 2. Fetch and update counselor rating (US-20)
            com.google.firebase.firestore.DocumentSnapshot counselorSnap = transaction.get(counselorRef);
            if (counselorSnap.exists()) {
                double oldAvg = counselorSnap.getDouble("averageRating") != null ? counselorSnap.getDouble("averageRating") : 0.0;
                long oldCount = counselorSnap.getLong("reviewCount") != null ? counselorSnap.getLong("reviewCount") : 0;
                
                long newCount = oldCount + 1;
                double newAvg = (oldAvg * oldCount + review.getRating()) / newCount;
                
                // Round to 1 decimal place for threshold check
                newAvg = Math.round(newAvg * 10.0) / 10.0;

                transaction.update(counselorRef, 
                        "averageRating", newAvg,
                        "reviewCount", newCount);

                // Auto-suspend if below threshold (US-20)
                if (newAvg < 2.5) {
                    transaction.update(counselorRef, "suspended", true);
                    activityController.logActivity("SUSPENSION", "Counselor " + review.getCounselorId() + " automatically suspended due to low rating: " + newAvg, "System");
                }
            }
            return null;
        }).addOnSuccessListener(v -> {
            activityController.logActivity("REVIEW", "New anonymous review submitted for counselor " + review.getCounselorId(), "Student");
            cb.onSuccess();
        }).addOnFailureListener(cb::onFailure);
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