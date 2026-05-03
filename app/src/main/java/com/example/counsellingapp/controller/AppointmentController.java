package com.example.counsellingapp.controller;

import android.content.Context;
import com.example.counsellingapp.model.Appointment;
import com.example.counsellingapp.model.Counselor;
import com.example.counsellingapp.model.Student;
import com.example.counsellingapp.model.TimeSlot;
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
    private final ActivityController activityController = new ActivityController();


    private static final String COL_APPOINTMENTS = "appointments";
    private static final String COL_STUDENTS     = "students";     // was "users" — fixed
    private static final String COL_COUNSELORS   = "counselors";   // was "users" — fixed
    private static final String COL_AVAILABILITY = "availability";

    /**
     * Callback interface for appointment list fetching operations.
     */
    public interface AppointmentListCallback {
        void onSuccess(List<Appointment> appointments);
        void onFailure(Exception e);
    }

    /**
     * Callback interface for booking and management operations.
     */
    public interface BookingCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    // -------------------------------------------------------------------------
    // US-14: Upcoming appointments for a counselor
    // -------------------------------------------------------------------------

    /**
     * Fetches upcoming appointments for a counselor and resolves each collaborating
     * object (Student, Counselor, TimeSlot) before delivering to the callback.
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
                    if (raw.isEmpty()) { cb.onSuccess(raw); return; }
                    resolveCollaborators(raw, cb);
                })
                .addOnFailureListener(cb::onFailure);
    }

    // -------------------------------------------------------------------------
    // US-11: Student appointment history
    // -------------------------------------------------------------------------

    /**
     * Fetches all appointments for a student and resolves collaborators
     * (Counselor, TimeSlot) before delivering to the callback.
     *
     * @param studentId The unique ID of the student.
     * @param callback  The callback to handle success or failure.
     */
    public void getStudentAppointmentHistory(String studentId, AppointmentListCallback callback) {
        db.collection(COL_APPOINTMENTS)
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Appointment> appointments = snapshots.toObjects(Appointment.class);
                    if (appointments.isEmpty()) { callback.onSuccess(appointments); return; }
                    resolveCollaborators(appointments, callback);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // -------------------------------------------------------------------------
    // Collaborator resolution
    // -------------------------------------------------------------------------

    /**
     * For each Appointment, fires three parallel Firestore reads to fetch the
     * typed Student (from "students/"), Counselor (from "counselors/"), and TimeSlot.
     * Only calls cb.onSuccess() once ALL appointments are fully resolved.
     *
     * <p>Previously this method read both participants from a single "users/" collection
     * using {@code toObject(User.class)}, which caused compile errors because
     * {@link Appointment#setStudent} requires {@link Student} and
     * {@link Appointment#setCounselor} requires {@link Counselor}.
     * Fixed by reading from the correct typed collections.
     */
    private void resolveCollaborators(List<Appointment> appointments,
                                      AppointmentListCallback cb) {
        int total = appointments.size() * 3;
        AtomicInteger remaining    = new AtomicInteger(total);
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

            // Read student from "students/" — deserialize as Student (not User)
            db.collection(COL_STUDENTS).document(appt.getStudentId()).get()
                    .addOnSuccessListener(doc -> {
                        appt.setStudent(doc.toObject(Student.class));  // was toObject(User.class)
                        checkDone.run();
                    })
                    .addOnFailureListener(e -> {
                        if (errorReported.getAndIncrement() == 0) cb.onFailure(e);
                    });

            // Read counselor from "counselors/" — deserialize as Counselor (not User)
            db.collection(COL_COUNSELORS).document(appt.getCounselorId()).get()
                    .addOnSuccessListener(doc -> {
                        appt.setCounselor(doc.toObject(Counselor.class));  // was toObject(User.class)
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

    // -------------------------------------------------------------------------
    // US-04: Book a slot
    // -------------------------------------------------------------------------

    /**
     * Securely books a slot using a Firestore Transaction.
     * Also triggers a local notification upon success.
     */
    public void bookSlot(Context context, TimeSlot slot, String studentId, BookingCallback cb) {
        com.google.firebase.firestore.DocumentReference slotRef =
                db.collection(COL_AVAILABILITY).document(slot.getId());
        com.google.firebase.firestore.DocumentReference newApptRef =
                db.collection(COL_APPOINTMENTS).document();

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
                    newApptRef.getId(), studentId, slot.getCounselorId(), slot.getId(), "upcoming");
            appt.setCounselorName(slot.getCounselorName());
            transaction.set(newApptRef, appt);
            return null;
        }).addOnSuccessListener(v -> {
            String msg = "Session scheduled with " + slot.getCounselorName()
                    + " on " + slot.getDate() + " at " + slot.getStartTime();
            NotificationHelper.sendNotification(context, "Appointment Booked", msg);
            activityController.logActivity("BOOKING", "Student booked appointment with counselor " + slot.getCounselorName(), "System");
            cb.onSuccess();

        }).addOnFailureListener(cb::onFailure);
    }

    // -------------------------------------------------------------------------
    // US-05: Cancel
    // -------------------------------------------------------------------------

    /** Student cancels an appointment and frees the timeslot. */
    public void cancelAppointment(String appointmentId, String timeslotId, BookingCallback cb) {
        com.google.firebase.firestore.DocumentReference apptRef =
                db.collection(COL_APPOINTMENTS).document(appointmentId);
        com.google.firebase.firestore.DocumentReference slotRef =
                db.collection(COL_AVAILABILITY).document(timeslotId);

        db.runTransaction((com.google.firebase.firestore.Transaction.Function<Void>) transaction -> {
            transaction.update(apptRef, "status", "cancelled");
            transaction.update(slotRef, "booked", false);
            return null;
        }).addOnSuccessListener(v -> {
            activityController.logActivity("CANCELLATION", "Student cancelled appointment " + appointmentId, "Student");
            cb.onSuccess();
        })

          .addOnFailureListener(cb::onFailure);
    }

    /** Counselor cancels an appointment and notifies the student. */
    public void cancelAppointmentByCounselor(Context context, Appointment appt, BookingCallback cb) {
        com.google.firebase.firestore.DocumentReference apptRef =
                db.collection(COL_APPOINTMENTS).document(appt.getId());
        com.google.firebase.firestore.DocumentReference slotRef =
                db.collection(COL_AVAILABILITY).document(appt.getTimeslotId());

        db.runTransaction((com.google.firebase.firestore.Transaction.Function<Void>) transaction -> {
            transaction.update(apptRef, "status", "cancelled");
            transaction.update(slotRef, "booked", false);
            return null;
        }).addOnSuccessListener(v -> {
            String msg = "Your appointment with Counselor " + appt.getCounselorName() + " has been cancelled.";
            NotificationHelper.sendNotification(context, "Appointment Cancelled", msg);
            activityController.logActivity("CANCELLATION", "Counselor " + appt.getCounselorName() + " cancelled appointment for student", "Counselor");
            cb.onSuccess();

        }).addOnFailureListener(cb::onFailure);
    }

    // -------------------------------------------------------------------------
    // US-05: Reschedule
    // -------------------------------------------------------------------------

    /** Student reschedules an appointment to a new timeslot. */
    public void rescheduleAppointment(Context context, String appointmentId,
                                      String oldTimeslotId, TimeSlot newSlot,
                                      BookingCallback cb) {
        com.google.firebase.firestore.DocumentReference oldApptRef =
                db.collection(COL_APPOINTMENTS).document(appointmentId);
        com.google.firebase.firestore.DocumentReference newApptRef =
                db.collection(COL_APPOINTMENTS).document();
        com.google.firebase.firestore.DocumentReference oldSlotRef =
                db.collection(COL_AVAILABILITY).document(oldTimeslotId);
        com.google.firebase.firestore.DocumentReference newSlotRef =
                db.collection(COL_AVAILABILITY).document(newSlot.getId());

        db.runTransaction((com.google.firebase.firestore.Transaction.Function<Void>) transaction -> {
            com.google.firebase.firestore.DocumentSnapshot oldAppt = transaction.get(oldApptRef);
            com.google.firebase.firestore.DocumentSnapshot slotSnap = transaction.get(newSlotRef);
            if (Boolean.TRUE.equals(slotSnap.getBoolean("booked"))) {
                throw new com.google.firebase.firestore.FirebaseFirestoreException(
                        "Slot taken", com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED);
            }
            transaction.update(oldApptRef, "status", "cancelled");
            transaction.update(oldSlotRef, "booked", false);
            Appointment newAppt = new Appointment(
                    newApptRef.getId(), oldAppt.getString("studentId"),
                    newSlot.getCounselorId(), newSlot.getId(), "upcoming");
            newAppt.setCounselorName(newSlot.getCounselorName());
            transaction.set(newApptRef, newAppt);
            transaction.update(newSlotRef, "booked", true);
            return null;
        }).addOnSuccessListener(v -> {
            NotificationHelper.sendNotification(context, "Appointment Rescheduled",
                    "You moved your appointment to " + newSlot.getDate());
            activityController.logActivity("RESCHEDULE", "Student rescheduled appointment to " + newSlot.getDate(), "Student");
            cb.onSuccess();

        }).addOnFailureListener(cb::onFailure);
    }

    /** Counselor reschedules an appointment and notifies the student. */
    public void rescheduleAppointmentByCounselor(Context context, Appointment appt,
                                                  TimeSlot newSlot, BookingCallback cb) {
        com.google.firebase.firestore.DocumentReference oldApptRef =
                db.collection(COL_APPOINTMENTS).document(appt.getId());
        com.google.firebase.firestore.DocumentReference newApptRef =
                db.collection(COL_APPOINTMENTS).document();
        com.google.firebase.firestore.DocumentReference oldSlotRef =
                db.collection(COL_AVAILABILITY).document(appt.getTimeslotId());
        com.google.firebase.firestore.DocumentReference newSlotRef =
                db.collection(COL_AVAILABILITY).document(newSlot.getId());

        db.runTransaction((com.google.firebase.firestore.Transaction.Function<Void>) transaction -> {
            com.google.firebase.firestore.DocumentSnapshot slotSnap = transaction.get(newSlotRef);
            if (Boolean.TRUE.equals(slotSnap.getBoolean("booked"))) {
                throw new com.google.firebase.firestore.FirebaseFirestoreException(
                        "Slot taken", com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED);
            }
            transaction.update(oldApptRef, "status", "rescheduled");
            transaction.update(oldSlotRef, "booked", false);
            Appointment newAppt = new Appointment(
                    newApptRef.getId(), appt.getStudentId(),
                    newSlot.getCounselorId(), newSlot.getId(), "upcoming");
            newAppt.setCounselorName(newSlot.getCounselorName());
            transaction.set(newApptRef, newAppt);
            transaction.update(newSlotRef, "booked", true);
            return null;
        }).addOnSuccessListener(v -> {
            String msg = "Counselor " + appt.getCounselorName()
                    + " rescheduled your session to " + newSlot.getDate();
            NotificationHelper.sendNotification(context, "Appointment Rescheduled", msg);
            cb.onSuccess();
        }).addOnFailureListener(cb::onFailure);
    }

    // -------------------------------------------------------------------------
    // Mark completed
    // -------------------------------------------------------------------------

    /**
     * Marks an appointment as completed in Firestore.
     * Called by the counselor from their dashboard appointment list.
     */
    public void markAsCompleted(String appointmentId, BookingCallback cb) {
        db.collection(COL_APPOINTMENTS)
                .document(appointmentId)
                .update("status", "completed")
                .addOnSuccessListener(v -> {
                    activityController.logActivity("COMPLETED", "Counselor marked appointment " + appointmentId + " as completed", "Counselor");
                    cb.onSuccess();
                })

                .addOnFailureListener(cb::onFailure);
    }

    // -------------------------------------------------------------------------
    // US-09: Student upcoming appointments (for calendar highlighting)
    // -------------------------------------------------------------------------

    /**
     * Fetches upcoming appointments for a student and resolves collaborators.
     * Used by StudentDashboardActivity to highlight the next appointment on the CalendarView.
     *
     * @param studentId The unique ID of the student.
     * @param callback  The callback to handle success or failure.
     */
    public void getStudentUpcomingAppointments(String studentId, AppointmentListCallback callback) {
        db.collection(COL_APPOINTMENTS)
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("status", "upcoming")
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Appointment> appointments = snapshots.toObjects(Appointment.class);
                    if (appointments.isEmpty()) { callback.onSuccess(appointments); return; }
                    resolveCollaborators(appointments, callback);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // -------------------------------------------------------------------------
    // US-17: Counselor attaches material to appointment
    // -------------------------------------------------------------------------

    /**
     * Updates the materials list for an appointment in Firestore.
     * Called by the counselor from their dashboard to attach resources.
     *
     * @param appointmentId The Firestore document ID of the appointment.
     * @param materials     The full updated list of material strings.
     * @param cb            Fires onSuccess() on write completion, onFailure() on error.
     */
    public void addMaterialToAppointment(String appointmentId, List<String> materials, BookingCallback cb) {
        db.collection(COL_APPOINTMENTS)
                .document(appointmentId)
                .update("materials", materials)
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onFailure);
    }
}

