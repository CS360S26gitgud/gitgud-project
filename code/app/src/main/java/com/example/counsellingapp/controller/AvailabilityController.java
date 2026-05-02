package com.example.counsellingapp.controller;

import com.example.counsellingapp.model.TimeSlot;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Handles Firestore CRUD operations for counselor availability (TimeSlot).
 */
public class AvailabilityController {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION = "availability";

    /**
     * Adds a new availability slot for the given counselor.
     * Also updates the counselor's availableDays field in the users collection
     * so that day-based search filtering works correctly.
     */
    public void addSlot(String counselorId, String date,
                        String startTime, String endTime,
                        AvailabilityCallback cb) {

        DocumentReference ref = db.collection(COLLECTION).document();
        TimeSlot slot = new TimeSlot(ref.getId(), counselorId, date, startTime, endTime);

        ref.set(slot)
                .addOnSuccessListener(v -> {
                    // Derive the day of week from the date string (format: YYYY-MM-DD)
                    String dayOfWeek = getDayOfWeek(date);

                    if (dayOfWeek == null) {
                        // Date parsing failed — slot is saved, just skip the day update
                        cb.onSuccess();
                        return;
                    }

                    // Add the day to the counselor's availableDays array in users collection.
                    // arrayUnion() is safe — it only adds the day if it isn't already there.
                    db.collection("users").document(counselorId)
                            .update("availableDays", FieldValue.arrayUnion(dayOfWeek))
                            .addOnSuccessListener(unused -> cb.onSuccess())
                            .addOnFailureListener(cb::onFailure);
                })
                .addOnFailureListener(cb::onFailure);
    }

    /**
     * Derives the full day name (e.g. "Thursday") from a date string in YYYY-MM-DD format.
     * Returns null if parsing fails.
     */
    private String getDayOfWeek(String date) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date parsed = sdf.parse(date);
            Calendar cal = Calendar.getInstance();
            cal.setTime(parsed);
            return new SimpleDateFormat("EEEE", Locale.getDefault()).format(cal.getTime());
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Replaces every field of an existing slot document.
     * Used when a counselor edits a previously saved slot.
     */
    public void updateSlot(String slotId, String counselorId, String date,
                           String startTime, String endTime,
                           AvailabilityCallback cb) {

        TimeSlot updated = new TimeSlot(slotId, counselorId, date, startTime, endTime);
        db.collection(COLLECTION).document(slotId)
                .set(updated)
                .addOnSuccessListener(v -> {
                    // Also ensure the day is recorded in the user's availableDays
                    String dayOfWeek = getDayOfWeek(date);
                    if (dayOfWeek == null) {
                        cb.onSuccess();
                        return;
                    }
                    db.collection("users").document(counselorId)
                            .update("availableDays", FieldValue.arrayUnion(dayOfWeek))
                            .addOnSuccessListener(unused -> cb.onSuccess())
                            .addOnFailureListener(cb::onFailure);
                })
                .addOnFailureListener(cb::onFailure);
    }

    /**
     * Loads all unbooked slots belonging to the given counselor,
     * ordered by date then startTime (client-side sort — no composite index needed).
     */
    public void getCounselorSlots(String counselorId, SlotListCallback cb) {
        db.collection(COLLECTION)
                .whereEqualTo("counselorId", counselorId)
                .whereEqualTo("booked", false)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<TimeSlot> slots = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        slots.add(doc.toObject(TimeSlot.class));
                    }
                    slots.sort((a, b) -> {
                        int cmp = a.getDate().compareTo(b.getDate());
                        return cmp != 0 ? cmp : a.getStartTime().compareTo(b.getStartTime());
                    });
                    cb.onSuccess(slots);
                })
                .addOnFailureListener(cb::onFailure);
    }

    public void getAllAvailableSlots(SlotListCallback cb) {
        db.collection(COLLECTION)
                .whereEqualTo("booked", false)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<TimeSlot> slots = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        slots.add(doc.toObject(TimeSlot.class));
                    }
                    if (slots.isEmpty()) {
                        cb.onSuccess(slots);
                        return;
                    }
                    // Hydrate names by fetching users collection once
                    db.collection("users").get().addOnSuccessListener(usersSnap -> {
                        java.util.Map<String, String> nameMap = new java.util.HashMap<>();
                        for (QueryDocumentSnapshot uDoc : usersSnap) {
                            nameMap.put(uDoc.getString("uid"), uDoc.getString("name"));
                        }
                        for (TimeSlot s : slots) {
                            String name = nameMap.get(s.getCounselorId());
                            s.setCounselorName(name != null ? name : "Unknown");
                        }
                        slots.sort((a, b) -> {
                            int cmp = a.getDate().compareTo(b.getDate());
                            return cmp != 0 ? cmp : a.getStartTime().compareTo(b.getStartTime());
                        });
                        cb.onSuccess(slots);
                    }).addOnFailureListener(cb::onFailure);
                })
                .addOnFailureListener(cb::onFailure);
    }

    public interface SlotListCallback {
        void onSuccess(List<TimeSlot> slots);
        void onFailure(Exception e);
    }

    public interface BookingCallback {
        void onSuccess();
        void onFailure(Exception e);
    }
}