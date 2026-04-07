package com.example.counsellingapp.controller;

import com.example.counsellingapp.model.Appointment;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.List;

/**
 * Controller for managing appointments.
 * Handles US 11: Viewing appointment history.
 */
public class AppointmentController {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface AppointmentListCallback {
        void onSuccess(List<Appointment> appointments);
        void onFailure(Exception e);
    }

    public void getStudentAppointmentHistory(String studentId, AppointmentListCallback callback) {
        db.collection("appointments")
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