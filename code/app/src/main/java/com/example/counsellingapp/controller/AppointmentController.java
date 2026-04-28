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

    /**
     * Callback interface for appointment list fetching operations.
     */
    public interface AppointmentListCallback {
        /**
         * Invoked when appointments are successfully fetched and resolved.
         * @param appointments The list of resolved Appointment objects.
         */
        void onSuccess(List<Appointment> appointments);

        /**
         * Invoked when the fetch operation fails.
         * @param e The exception that occurred.
         */
        void onFailure(Exception e);
    }

    /**
     * US-14: Fetches upcoming appointments for a counselor and resolves
     * each collaborating object (Student, Counselor, TimeSlot) before
     * delivering to the callback.
     *
     * @param counselorId The unique ID of the counselor.
     * @param cb          The callback to handle success or failure.
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
     *
     * @param appointments The list of raw appointment objects to resolve.
     * @param cb           The callback to trigger once resolution is complete.
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

    /**
     * Callback interface for booking and management operations.
     */
    public interface BookingCallback {
        /**
         * Invoked when the operation completes successfully.
         */
        void onSuccess();

        /**
         * Invoked when the operation fails.
         * @param e The exception that occurred.
         */
        void onFailure(Exception e);
    }

    /**
     * US-04: Securely books a slot using a Firestore Transaction.
     * Also triggers a local notification upon success.
     *
     * @param context   The context to trigger the notification.
     * @param slot      The TimeSlot to book.
     * @param studentId The ID of the student booking the slot.
     * @param cb        The callback to handle the result.
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
     * @param appointmentId The unique ID of the appointment to cancel.
     * @param timeslotId    The unique ID of the timeslot to free.
     * @param cb            The callback to handle the result.
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
     * Counselor cancels an appointment and notifies the student.
     *
     * @param context The context for notification.
     * @param appt    The appointment object being cancelled.
     * @param cb      The callback to handle the result.
     */
    public void cancelAppointmentByCounselor(Context context, Appointment appt, BookingCallback cb) {
        com.google.firebase.firestore.DocumentReference apptRef = db.collection(COL_APPOINTMENTS).document(appt.getId());
        com.google.firebase.firestore.DocumentReference slotRef = db.collection(COL_AVAILABILITY).document(appt.getTimeslotId());

        db.runTransaction((com.google.firebase.firestore.Transaction.Function<Void>) transaction -> {
            transaction.update(apptRef, "status", "cancelled");
            transaction.update(slotRef, "booked", false);
            return null;
        }).addOnSuccessListener(v -> {
            String msg = "Your appointment with Counselor " + appt.getCounselorName() + " has been cancelled.";
            NotificationHelper.sendNotification(context, "Appointment Cancelled", msg);
            cb.onSuccess();
        }).addOnFailureListener(cb::onFailure);
    }

    /**
     * US-05: Reschedules an appointment to a new timeslot (Student-initiated).
     * Creates a new appointment and marks the old one as "cancelled" for transparency.
     *
     * @param context       The context for notification.
     * @param appointmentId The ID of the existing appointment.
     * @param oldTimeslotId The ID of the old timeslot.
     * @param newSlot       The new TimeSlot object.
     * @param cb            The callback to handle the result.
     */
    public void rescheduleAppointment(Context context, String appointmentId, String oldTimeslotId, TimeSlot newSlot, BookingCallback cb) {
        com.google.firebase.firestore.DocumentReference oldApptRef = db.collection(COL_APPOINTMENTS).document(appointmentId);
        com.google.firebase.firestore.DocumentReference newApptRef = db.collection(COL_APPOINTMENTS).document();
        com.google.firebase.firestore.DocumentReference oldSlotRef = db.collection(COL_AVAILABILITY).document(oldTimeslotId);
        com.google.firebase.firestore.DocumentReference newSlotRef = db.collection(COL_AVAILABILITY).document(newSlot.getId());

        db.runTransaction((com.google.firebase.firestore.Transaction.Function<Void>) transaction -> {
            com.google.firebase.firestore.DocumentSnapshot oldAppt = transaction.get(oldApptRef);
            com.google.firebase.firestore.DocumentSnapshot slotSnap = transaction.get(newSlotRef);

            if (Boolean.TRUE.equals(slotSnap.getBoolean("booked"))) {
                throw new com.google.firebase.firestore.FirebaseFirestoreException("Slot taken", com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED);
            }

            // Mark old as cancelled (Student side transparency)
            transaction.update(oldApptRef, "status", "cancelled");
            transaction.update(oldSlotRef, "booked", false);

            // Create new appointment
            Appointment newAppt = new Appointment(
                    newApptRef.getId(), oldAppt.getString("studentId"), 
                    newSlot.getCounselorId(), newSlot.getId(), "upcoming"
            );
            newAppt.setCounselorName(newSlot.getCounselorName());
            transaction.set(newApptRef, newAppt);
            transaction.update(newSlotRef, "booked", true);

            return null;
        }).addOnSuccessListener(v -> {
            NotificationHelper.sendNotification(context, "Appointment Rescheduled", "You moved your appointment to " + newSlot.getDate());
            cb.onSuccess();
        }).addOnFailureListener(cb::onFailure);
    }

    /**
     * Counselor reschedules an appointment and notifies the student.
     * Creates a new appointment and marks the old one as "rescheduled" for student transparency.
     *
     * @param context The context for notification.
     * @param appt    The appointment object being moved.
     * @param newSlot The new TimeSlot object.
     * @param cb      The callback to handle the result.
     */
    public void rescheduleAppointmentByCounselor(Context context, Appointment appt, TimeSlot newSlot, BookingCallback cb) {
        com.google.firebase.firestore.DocumentReference oldApptRef = db.collection(COL_APPOINTMENTS).document(appt.getId());
        com.google.firebase.firestore.DocumentReference newApptRef = db.collection(COL_APPOINTMENTS).document();
        com.google.firebase.firestore.DocumentReference oldSlotRef = db.collection(COL_AVAILABILITY).document(appt.getTimeslotId());
        com.google.firebase.firestore.DocumentReference newSlotRef = db.collection(COL_AVAILABILITY).document(newSlot.getId());

        db.runTransaction((com.google.firebase.firestore.Transaction.Function<Void>) transaction -> {
            com.google.firebase.firestore.DocumentSnapshot slotSnap = transaction.get(newSlotRef);
            if (Boolean.TRUE.equals(slotSnap.getBoolean("booked"))) {
                throw new com.google.firebase.firestore.FirebaseFirestoreException("Slot taken", com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED);
            }

            // Mark old as Rescheduled for transparency
            transaction.update(oldApptRef, "status", "rescheduled");
            transaction.update(oldSlotRef, "booked", false);

            // Create new appointment
            Appointment newAppt = new Appointment(
                    newApptRef.getId(), appt.getStudentId(), 
                    newSlot.getCounselorId(), newSlot.getId(), "upcoming"
            );
            newAppt.setCounselorName(newSlot.getCounselorName());
            transaction.set(newApptRef, newAppt);
            transaction.update(newSlotRef, "booked", true);

            return null;
        }).addOnSuccessListener(v -> {
            String msg = "Counselor " + appt.getCounselorName() + " rescheduled your session to " + newSlot.getDate();
            NotificationHelper.sendNotification(context, "Appointment Rescheduled", msg);
            cb.onSuccess();
        }).addOnFailureListener(cb::onFailure);
    }

    /**
     * US-11: Fetches all appointments for a student and resolves
     * collaborators (Counselor, TimeSlot) before delivering to the callback.
     *
     * @param studentId The unique ID of the student.
     * @param callback  The callback to handle success or failure.
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
