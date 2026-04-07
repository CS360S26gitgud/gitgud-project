package com.example.counsellingapp.controller;

import com.example.counsellingapp.model.Appointment;
import com.example.counsellingapp.model.TimeSlot;
import com.example.counsellingapp.model.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles Firestore operations for Appointments.
 *
 * Key change from previous version: after loading raw appointment documents,
 * the controller resolves each studentId, counselorId and timeslotId into full
 * objects and attaches them to the Appointment — honouring the CRC collaborator
 * relationships (Student, Counselor, TimeSlot).
 *
 * The View receives fully-hydrated Appointment objects and never talks to
 * Firestore directly.
 */
public class AppointmentController {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private static final String COL_APPOINTMENTS = "appointments";
    private static final String COL_USERS        = "users";
    private static final String COL_AVAILABILITY = "availability";

    public interface AppointmentListCallback {
        void onSuccess(List<Appointment> appointments);
        void onFailure(Exception e);
    }

    /**
     * US-14: Fetches upcoming appointments for a counselor and resolves
     * each collaborating object (Student, Counselor, TimeSlot) before
     * delivering to the callback.
     */
    public void getUpcomingForCounselor(String counselorId, AppointmentListCallback cb) {

        db.collection(COL_APPOINTMENTS)
                .whereEqualTo("counselorId", counselorId)
                .whereEqualTo("status", "upcoming")
                .get()
                .addOnSuccessListener(snapshots -> {

                    List<Appointment> raw = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        raw.add(doc.toObject(Appointment.class));
                    }

                    if (raw.isEmpty()) {
                        cb.onSuccess(raw);
                        return;
                    }

                    // Resolve collaborators for every appointment, then deliver
                    resolveCollaborators(raw, cb);
                })
                .addOnFailureListener(cb::onFailure);
    }

    /**
     * For each Appointment, fire three parallel Firestore reads to fetch the
     * Student (User), Counselor (User), and TimeSlot objects.
     * Only calls cb.onSuccess() once ALL appointments are fully resolved.
     *
     * AtomicInteger is used as a thread-safe countdown because Firestore
     * callbacks arrive on the main thread but we want to be safe.
     */
    private void resolveCollaborators(List<Appointment> appointments,
                                      AppointmentListCallback cb) {

        // Each appointment needs 3 reads; track total work remaining
        int total = appointments.size() * 3;
        AtomicInteger remaining = new AtomicInteger(total);

        // Single error flag — report only the first failure
        AtomicInteger errorReported = new AtomicInteger(0);

        Runnable checkDone = () -> {
            if (remaining.decrementAndGet() == 0) {
                // All reads finished — sort and deliver
                appointments.sort((a, b) -> {
                    if (a.getTimeSlot() == null || b.getTimeSlot() == null) return 0;
                    int cmp = a.getTimeSlot().getDate()
                            .compareTo(b.getTimeSlot().getDate());
                    return cmp != 0 ? cmp
                            : a.getTimeSlot().getStartTime()
                            .compareTo(b.getTimeSlot().getStartTime());
                });
                cb.onSuccess(appointments);
            }
        };

        for (Appointment appt : appointments) {

            //Resolve Student collaborator
            db.collection(COL_USERS).document(appt.getStudentId()).get()
                    .addOnSuccessListener(doc -> {
                        appt.setStudent(doc.toObject(User.class));
                        checkDone.run();
                    })
                    .addOnFailureListener(e -> {
                        if (errorReported.getAndIncrement() == 0) cb.onFailure(e);
                    });

            //Resolve Counselor collaborator
            db.collection(COL_USERS).document(appt.getCounselorId()).get()
                    .addOnSuccessListener(doc -> {
                        appt.setCounselor(doc.toObject(User.class));
                        checkDone.run();
                    })
                    .addOnFailureListener(e -> {
                        if (errorReported.getAndIncrement() == 0) cb.onFailure(e);
                    });

            //Resolve TimeSlot collaborator
            db.collection(COL_AVAILABILITY).document(appt.getTimeslotId()).get()
                    .addOnSuccessListener(doc -> {
                        appt.setTimeSlot(doc.toObject(TimeSlot.class));
                        checkDone.run();
                    })
                    .addOnFailureListener(e -> {
                        if (errorReported.getAndIncrement() == 0) cb.onFailure(e);
                    });
        }
    }

    public interface BookingCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    /**
     * US-04: Securely books a slot using a Firestore Transaction.
     */
    public void bookSlot(TimeSlot slot, String studentId, BookingCallback cb) {
        com.google.firebase.firestore.DocumentReference slotRef = db.collection(COL_AVAILABILITY).document(slot.getId());
        com.google.firebase.firestore.DocumentReference newApptRef = db.collection(COL_APPOINTMENTS).document();

        db.runTransaction((com.google.firebase.firestore.Transaction.Function<Void>) transaction -> {
                    com.google.firebase.firestore.DocumentSnapshot snapshot = transaction.get(slotRef);
                    Boolean isBooked = snapshot.getBoolean("booked");

                    // Checking one last time that no one stole the slot milliseconds before us!
                    if (isBooked != null && isBooked) {
                        throw new com.google.firebase.firestore.FirebaseFirestoreException(
                                "Slot was just taken!",
                                com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED);
                    }

                    // 1. Mark the slot as officially taken
                    transaction.update(slotRef, "booked", true);

                    // 2. Create the actual Appointment ticket
                    Appointment appt = new Appointment(
                            newApptRef.getId(), studentId, slot.getCounselorId(), slot.getId(), "upcoming"
                    );
                    transaction.set(newApptRef, appt);

                    return null;
                }).addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onFailure);
    }

    public void getStudentAppointmentHistory(String studentId, AppointmentListCallback callback) {
        db.collection(COL_APPOINTMENTS)
                .whereEqualTo("studentId", studentId)
                .orderBy("dateTime", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Appointment> appointments = queryDocumentSnapshots.toObjects(Appointment.class);
                    callback.onSuccess(appointments);
                })
                .addOnFailureListener(callback::onFailure);
    }
}