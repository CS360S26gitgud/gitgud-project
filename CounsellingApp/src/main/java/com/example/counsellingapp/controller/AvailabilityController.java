package com.example.counsellingapp.controller;

import com.example.counsellingapp.model.Appointment;
import com.example.counsellingapp.model.TimeSlot;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles Firestore CRUD operations for counselor availability (TimeSlot).
 * CRC responsibility: Represent a specific date/time window;
 *                     support recurring slot creation.
 * Outstanding issue: recurring/repeat-slot logic not yet implemented.
 */
public class AvailabilityController {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION = "availability";

    /**
     * Adds a new availability slot for the given counselor.
     * Auto-generates the Firestore document ID and stores it inside the object.
     */
    public void addSlot(String counselorId, String date,
                        String startTime, String endTime,
                        AvailabilityCallback cb) {

        DocumentReference ref = db.collection(COLLECTION).document();
        TimeSlot slot = new TimeSlot(ref.getId(), counselorId, date, startTime, endTime);

        ref.set(slot)
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onFailure);
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
                .addOnSuccessListener(v -> cb.onSuccess())
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
                        TimeSlot s = doc.toObject(TimeSlot.class);
                        slots.add(s);
                    }
                    // Sort by date, then startTime so UI is always chronological
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
                            s.setCounselorName(name != null ? name : "Unknown ID");
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


    /** Separate typed callback for list results. */
    public interface SlotListCallback {
        void onSuccess(List<TimeSlot> slots);
        void onFailure(Exception e);
    }

    public interface BookingCallback {
        void onSuccess();
        void onFailure(Exception e);
    }



}