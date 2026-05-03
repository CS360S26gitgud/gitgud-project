package com.example.counsellingapp.controller;

import com.example.counsellingapp.model.SystemActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for tracking and retrieving system-wide activity logs (US-21).
 */
public class ActivityController {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COL_ACTIVITIES = "activities";

    public interface ActivityListCallback {
        void onSuccess(List<SystemActivity> activities);
        void onFailure(Exception e);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    /**
     * Logs a new system event to Firestore.
     *
     * @param type          Type of event (BOOKING, CANCELLATION, etc.)
     * @param description   Detailed description of the event.
     * @param initiatorName Name of the person who triggered the event.
     */
    public void logActivity(String type, String description, String initiatorName) {
        SystemActivity activity = new SystemActivity(type, description, initiatorName);
        db.collection(COL_ACTIVITIES)
                .add(activity)
                .addOnSuccessListener(docRef -> {
                    // Successfully logged
                })
                .addOnFailureListener(e -> {
                    // Failed to log - usually we don't want to block the user for log failures
                });
    }

    /**
     * Fetches the most recent system activities, sorted by timestamp (newest first).
     *
     * @param limit Number of recent activities to fetch.
     * @param cb    Callback to deliver the list.
     */
    public void getRecentActivities(int limit, ActivityListCallback cb) {
        db.collection(COL_ACTIVITIES)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<SystemActivity> activities = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        SystemActivity activity = doc.toObject(SystemActivity.class);
                        activity.setId(doc.getId());
                        activities.add(activity);
                    }
                    cb.onSuccess(activities);
                })
                .addOnFailureListener(cb::onFailure);
    }
}
