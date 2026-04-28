package com.example.counsellingapp.controller;

import android.content.Context;
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
 * Controller handling appointment-related business logic and Firestore operations.
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
     *
     * @param counselorId The ID of the counselor.
     * @param cb          Callback for success or failure.
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

                    resolveCollaborators(raw, cb);
                })
                .addOnFailureListener(cb::onFailure);
    }

    /**
     * For each Appointment, fire three parallel Firestore reads to fetch the
     * Student (User), Counselor (User), and TimeSlot objects.
     * Only calls cb.onSuccess() once ALL appointments are fully resolved.
     */
    private void resolveCollaborators(List<Appointment> appointments,
                                      AppointmentListCallback cb) {

        int total = appointments.size() * 3;
        AtomicInteger remaining = new AtomicInteger(total);
        AtomicInteger errorReported = new AtomicInteger(0);

        Runnable checkDone = () -> {
            if (remaining.decrementAndGet() == 0) {
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

            db.collection(COL_USERS).document(appt.getStudentId()).get()
                    .addOnSuccessListener(doc -> {
                        appt.setStudent(doc.toObject(User.class));
                        checkDone.run();
                    })
                    .addOnFailureListener(e -> {
                        if (errorReported.getAndIncrement() == 0) cb.onFailure(e);
                    });

            db.collection(COL_USERS).document(appt.getCounselorId()).get()
                    .addOnSuccessListener(doc -> {
                        appt.setCounselor(doc.toObject(User.class));
                        checkDone.run();
                    })
                    .addOnFailureListener(e -> {
                        if (errorReported.getAndIncrement() == 0) cb.onFailure(e);
                    });

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
     * Also triggers a local notification upon success.
     *
     * @param context   The context to trigger the notification.
     * @param slot      The TimeSlot to book.
     * @param studentId The ID of the student booking the slot.
     * @param cb        Callback for success or failure.
     */
    public void bookSlot(Context context, TimeSlot slot, String studentId, BookingCallback cb) {
        com.google.firebase.firestore.DocumentReference slotRef = db.collection(COL_AVAILABILITY).document(slot.getId());
        com.google.firebase.firestore.DocumentReference newApptRef = db.collection(COL_APPOINTMENTS).document();

        db.runTransaction((com.google.firebase.firestore.Transaction.Function<Void>) transaction -> {
                    com.google.firebase.firestore.DocumentSnapshot snapshot = transaction.get(slotRef);
                    Boolean isBooked = snapshot.getBoolean("booked");

                    if (isBooked != null && isBooked) {
                        throw new com.google.firebase.firestore.FirebaseFirestoreException(
                                "Slot was just taken!",
                                com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED);
                    }

                    transaction.update(slotRef, "booked", true);

                    Appointment appt = new Appointment(
                            newApptRef.getId(), studentId, slot.getCounselorId(), slot.getId(), "upcoming"
                    );
                    appt.setCounselorName(slot.getCounselorName());
                    transaction.set(newApptRef, appt);

                    return null;
                }).addOnSuccessListener(v -> {
                    // US-08: Trigger local notification
                    String msg = "Session scheduled with " + slot.getCounselorName() + 
                                 " on " + slot.getDate() + " at " + slot.getStartTime();
                    NotificationHelper.sendNotification(context, "Appointment Booked", msg);
                    cb.onSuccess();
                })
                .addOnFailureListener(cb::onFailure);
    }

    /**
     * US-05: Cancels an existing appointment and marks the associated timeslot as available.
     * Uses a transaction to ensure both updates succeed or fail together.
     *
     * @param appointmentId The ID of the appointment to cancel.
     * @param timeslotId    The ID of the timeslot associated with the appointment.
     * @param cb            Callback for success or failure.
     */
    public void cancelAppointment(String appointmentId, String timeslotId, BookingCallback cb) {
        com.google.firebase.firestore.DocumentReference apptRef = db.collection(COL_APPOINTMENTS).document(appointmentId);
        com.google.firebase.firestore.DocumentReference slotRef = db.collection(COL_AVAILABILITY).document(timeslotId);

        db.runTransaction((com.google.firebase.firestore.Transaction.Function<Void>) transaction -> {
            transaction.update(apptRef, "status", "cancelled");
            transaction.update(slotRef, "booked", false);
            return null;
        }).addOnSuccessListener(v -> cb.onSuccess())
          .addOnFailureListener(cb::onFailure);
    }

    /**
     * US-05: Reschedules an appointment to a new timeslot.
     * Uses a transaction to:
     * 1. Validate the new slot is available.
     * 2. Free the old slot.
     * 3. Book the new slot.
     * 4. Update the appointment record.
     *
     * @param context        The context to trigger the notification.
     * @param appointmentId  The ID of the appointment to reschedule.
     * @param oldTimeslotId The ID of the currently booked timeslot.
     * @param newSlot        The new TimeSlot object chosen by the student.
     * @param cb             Callback for success or failure.
     */
    public void rescheduleAppointment(Context context, String appointmentId, String oldTimeslotId, TimeSlot newSlot, BookingCallback cb) {
        com.google.firebase.firestore.DocumentReference apptRef = db.collection(COL_APPOINTMENTS).document(appointmentId);
        com.google.firebase.firestore.DocumentReference oldSlotRef = db.collection(COL_AVAILABILITY).document(oldTimeslotId);
        com.google.firebase.firestore.DocumentReference newSlotRef = db.collection(COL_AVAILABILITY).document(newSlot.getId());

        db.runTransaction((com.google.firebase.firestore.Transaction.Function<Void>) transaction -> {
            com.google.firebase.firestore.DocumentSnapshot snapshot = transaction.get(newSlotRef);
            Boolean isBooked = snapshot.getBoolean("booked");

            if (isBooked != null && isBooked) {
                throw new com.google.firebase.firestore.FirebaseFirestoreException(
                        "New slot was just taken!",
                        com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED);
            }

            transaction.update(oldSlotRef, "booked", false);
            transaction.update(newSlotRef, "booked", true);
            transaction.update(apptRef,
                    "timeslotId", newSlot.getId(),
                    "counselorId", newSlot.getCounselorId(),
                    "counselorName", newSlot.getCounselorName()
            );

            return null;
        }).addOnSuccessListener(v -> {
            // US-08: Trigger local notification for rescheduling
            String msg = "Session rescheduled with " + newSlot.getCounselorName() + 
                         " to " + newSlot.getDate() + " at " + newSlot.getStartTime();
            NotificationHelper.sendNotification(context, "Appointment Rescheduled", msg);
            cb.onSuccess();
        })
          .addOnFailureListener(cb::onFailure);
    }

    /**
     * US-11: Fetches all appointments for a student and resolves
     * collaborators (Counselor, TimeSlot) before delivering to the callback.
     *
     * @param studentId The ID of the student.
     * @param callback  Callback for success or failure.
     */
    public void getStudentAppointmentHistory(String studentId, AppointmentListCallback callback) {
        db.collection(COL_APPOINTMENTS)
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Appointment> appointments = queryDocumentSnapshots.toObjects(Appointment.class);

                    if (appointments.isEmpty()) {
                        callback.onSuccess(appointments);
                        return;
                    }

                    // Resolve collaborators before delivering — same pattern as getUpcomingForCounselor
                    resolveCollaborators(appointments, callback);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Marks an appointment as completed in Firestore.
     * Called by the counselor from their dashboard appointment list.
     * Once completed, the student will see a "Leave a Review" button
     * for this appointment in their history screen.
     *
     * @param appointmentId The Firestore document ID of the appointment to update.
     * @param cb            Fires onSuccess() on write completion, onFailure() on error.
     */
    public void markAsCompleted(String appointmentId, BookingCallback cb) {
        db.collection(COL_APPOINTMENTS)
                .document(appointmentId)
                .update("status", "completed")
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onFailure);
    }
}
