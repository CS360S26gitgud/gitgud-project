package com.example.counsellingapp.controller;

import com.example.counsellingapp.model.Counselor;
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
 * Handles Firestore CRUD operations for counselor availability ({@link TimeSlot}).
 *
 * <p>The key change from the original version is in {@link #getAllAvailableSlots}: it now
 * fetches counselor names from the {@code counselors/} collection instead of the former
 * shared {@code users/} collection, deserializing cleanly as {@link Counselor} objects
 * without needing a role discriminator.
 *
 * <p>Outstanding issue: recurring-slot creation deferred to a later sprint.
 *
 * <p>CRC Responsibilities:
 * <ul>
 *   <li>Create and update counselor availability slots in Firestore.
 *   <li>Keep the counselor's {@code availableDays} array in sync on every slot write.
 *   <li>Fetch available slots for display, hydrating counselor names from
 *       the {@code counselors/} collection.
 * </ul>
 *
 * CRC Collaborators: {@link TimeSlot}, {@link Counselor}, {@link SlotListCallback}
 */
public class AvailabilityController {

    private static final String COLLECTION_AVAILABILITY = "availability";
    private static final String COLLECTION_COUNSELORS   = "counselors";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // -------------------------------------------------------------------------
    // Callback interfaces
    // -------------------------------------------------------------------------

    /**
     * Callback for operations that return a list of {@link TimeSlot} objects.
     */
    public interface SlotListCallback {
        /**
         * Called when the slot list has been successfully retrieved.
         *
         * @param slots The resulting list. Never {@code null}; may be empty.
         */
        void onSuccess(List<TimeSlot> slots);

        /**
         * Called when the Firestore query fails.
         *
         * @param e The exception describing the failure.
         */
        void onFailure(Exception e);
    }

    /**
     * Callback for single-slot write operations.
     */
    public interface BookingCallback {
        /** Called when the write completes successfully. */
        void onSuccess();

        /**
         * Called when the write fails.
         *
         * @param e The exception describing the failure.
         */
        void onFailure(Exception e);
    }

    // -------------------------------------------------------------------------
    // Write operations
    // -------------------------------------------------------------------------

    /**
     * Adds a new availability slot for the given counselor and updates the
     * counselor's {@code availableDays} array in the {@code counselors/} collection
     * using {@code FieldValue.arrayUnion} (safe to call multiple times for the same day).
     *
     * <p>If date parsing fails the slot is still saved but the {@code availableDays}
     * update is skipped; {@link AvailabilityCallback#onSuccess} is still fired.
     *
     * @param counselorId Firestore document ID of the counselor adding the slot.
     * @param date        Slot date in {@code YYYY-MM-DD} format.
     * @param startTime   Slot start time in {@code HH:mm} (24-hour) format.
     * @param endTime     Slot end time in {@code HH:mm} (24-hour) format.
     * @param cb          Callback fired on completion or failure.
     */
    public void addSlot(String counselorId, String date,
                        String startTime, String endTime,
                        AvailabilityCallback cb) {
        DocumentReference ref = db.collection(COLLECTION_AVAILABILITY).document();
        TimeSlot slot = new TimeSlot(ref.getId(), counselorId, date, startTime, endTime);

        ref.set(slot)
                .addOnSuccessListener(v -> {
                    String dayOfWeek = getDayOfWeek(date);
                    if (dayOfWeek == null) { cb.onSuccess(); return; }
                    db.collection(COLLECTION_COUNSELORS).document(counselorId)
                            .update("availableDays", FieldValue.arrayUnion(dayOfWeek))
                            .addOnSuccessListener(unused -> cb.onSuccess())
                            .addOnFailureListener(cb::onFailure);
                })
                .addOnFailureListener(cb::onFailure);
    }

    /**
     * Fully replaces an existing {@link TimeSlot} document and ensures the corresponding
     * day is present in the counselor's {@code availableDays} array.
     *
     * @param slotId      Firestore document ID of the slot to replace.
     * @param counselorId Firestore document ID of the owning counselor.
     * @param date        New slot date in {@code YYYY-MM-DD} format.
     * @param startTime   New start time in {@code HH:mm} format.
     * @param endTime     New end time in {@code HH:mm} format.
     * @param cb          Callback fired on completion or failure.
     */
    public void updateSlot(String slotId, String counselorId, String date,
                           String startTime, String endTime,
                           AvailabilityCallback cb) {
        TimeSlot updated = new TimeSlot(slotId, counselorId, date, startTime, endTime);
        db.collection(COLLECTION_AVAILABILITY).document(slotId)
                .set(updated)
                .addOnSuccessListener(v -> {
                    String dayOfWeek = getDayOfWeek(date);
                    if (dayOfWeek == null) { cb.onSuccess(); return; }
                    db.collection(COLLECTION_COUNSELORS).document(counselorId)
                            .update("availableDays", FieldValue.arrayUnion(dayOfWeek))
                            .addOnSuccessListener(unused -> cb.onSuccess())
                            .addOnFailureListener(cb::onFailure);
                })
                .addOnFailureListener(cb::onFailure);
    }

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

    /**
     * Loads all unbooked slots belonging to the given counselor, sorted by date
     * then start time (client-side — no composite Firestore index required).
     *
     * @param counselorId Firestore document ID of the counselor whose slots to fetch.
     * @param cb          Delivers the sorted list on success.
     */
    public void getCounselorSlots(String counselorId, SlotListCallback cb) {
        db.collection(COLLECTION_AVAILABILITY)
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

    /**
     * Loads all unbooked slots across all counselors and hydrates each slot with the
     * owning counselor's display name.
     *
     * <p>Name hydration fetches only the {@code counselors/} collection — previously
     * the entire {@code users/} collection was fetched for this step.
     *
     * <p>Result is sorted by date then start time on the client side.
     *
     * @param cb Delivers the sorted, name-hydrated list on success.
     */
    public void getAllAvailableSlots(SlotListCallback cb) {
        db.collection(COLLECTION_AVAILABILITY)
                .whereEqualTo("booked", false)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<TimeSlot> slots = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        slots.add(doc.toObject(TimeSlot.class));
                    }
                    if (slots.isEmpty()) { cb.onSuccess(slots); return; }

                    db.collection(COLLECTION_COUNSELORS)
                            .get()
                            .addOnSuccessListener(counselorSnap -> {
                                java.util.Map<String, String> nameMap = new java.util.HashMap<>();
                                for (QueryDocumentSnapshot cDoc : counselorSnap) {
                                    Counselor c = cDoc.toObject(Counselor.class);
                                    if (c.getUid() != null && c.getName() != null) {
                                        nameMap.put(c.getUid(), c.getName());
                                    }
                                }
                                for (TimeSlot s : slots) {
                                    String name = nameMap.get(s.getCounselorId());
                                    s.setCounselorName(name != null ? name : "Unknown");
                                }
                                slots.sort((a, b) -> {
                                    int cmp = a.getDate().compareTo(b.getDate());
                                    return cmp != 0 ? cmp
                                            : a.getStartTime().compareTo(b.getStartTime());
                                });
                                cb.onSuccess(slots);
                            })
                            .addOnFailureListener(cb::onFailure);
                })
                .addOnFailureListener(cb::onFailure);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Derives the full English day name (e.g. {@code "Thursday"}) from a
     * {@code YYYY-MM-DD} date string.
     *
     * @param date A date string in {@code YYYY-MM-DD} format.
     * @return The full day name, or {@code null} if {@code date} cannot be parsed.
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
}