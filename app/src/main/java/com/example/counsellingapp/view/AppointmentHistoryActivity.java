package com.example.counsellingapp.view;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.counsellingapp.R;
import com.example.counsellingapp.controller.AppointmentController;
import com.example.counsellingapp.controller.AvailabilityController;
import com.example.counsellingapp.controller.ReviewController;
import com.example.counsellingapp.model.Appointment;
import com.example.counsellingapp.model.TimeSlot;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Activity for students to view their appointment history.
 * Supports viewing upcoming, completed, and cancelled appointments.
 * Integrated with US-06 (Reviews), US-05 (Cancel/Reschedule), and US-08 (Notifications).
 * 
 * CRC Responsibilities:
 * - Display list of student appointments
 * - Provide hooks for cancelling and rescheduling upcoming appointments
 * - Provide hooks for reviewing completed appointments
 * 
 * Collaborators: AppointmentController, HistoryAdapter, AvailabilityController, ReviewController
 */
public class AppointmentHistoryActivity extends BaseSessionActivity implements HistoryAdapter.OnAppointmentInteractionListener {

    private RecyclerView rvHistory;
    private ProgressBar  progressBar;
    private TextView     tvEmpty;

    private AppointmentController appointmentController;
    private ReviewController      reviewController;
    private AvailabilityController availabilityController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_history);

        appointmentController = new AppointmentController();
        reviewController      = new ReviewController();
        availabilityController = new AvailabilityController();

        rvHistory   = findViewById(R.id.rvHistory);
        progressBar = findViewById(R.id.progressBarHistory);
        tvEmpty     = findViewById(R.id.tvNoHistory);

        rvHistory.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the list whenever the activity becomes visible
        loadHistory();
    }

    /**
     * Fetches the appointment history for the currently logged-in student.
     * Resolves collaborator objects and checks for existing reviews before rendering.
     */
    private void loadHistory() {
        String studentId = FirebaseAuth.getInstance().getUid();
        if (studentId == null) return;

        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        rvHistory.setVisibility(View.GONE);

        appointmentController.getStudentAppointmentHistory(studentId,
                new AppointmentController.AppointmentListCallback() {

                    @Override
                    public void onSuccess(List<Appointment> appointments) {
                        if (appointments.isEmpty()) {
                            progressBar.setVisibility(View.GONE);
                            tvEmpty.setVisibility(View.VISIBLE);
                            return;
                        }

                        List<String> completedIds = appointments.stream()
                                .filter(a -> "completed".equals(a.getStatus()))
                                .map(Appointment::getId)
                                .collect(Collectors.toList());

                        if (completedIds.isEmpty()) {
                            progressBar.setVisibility(View.GONE);
                            renderList(appointments, new HashSet<>());
                            return;
                        }

                        reviewController.getReviewedAppointmentIds(completedIds,
                                new ReviewController.ReviewedIdsCallback() {
                                    @Override
                                    public void onSuccess(Set<String> reviewedIds) {
                                        progressBar.setVisibility(View.GONE);
                                        renderList(appointments, reviewedIds);
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        progressBar.setVisibility(View.GONE);
                                        renderList(appointments, new HashSet<>());
                                    }
                                });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(AppointmentHistoryActivity.this,
                                "Failed to load history: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Renders the list of appointments using the HistoryAdapter.
     * 
     * @param appointments List of resolved Appointment objects.
     * @param reviewedIds  Set of appointment IDs that have already been reviewed.
     */
    private void renderList(List<Appointment> appointments, Set<String> reviewedIds) {
        rvHistory.setVisibility(View.VISIBLE);
        rvHistory.setAdapter(new HistoryAdapter(appointments, reviewedIds, this));
    }

    /**
     * Implementation of HistoryAdapter.OnAppointmentInteractionListener.
     * Displays a confirmation dialog before cancelling an appointment.
     * 
     * @param appt The appointment to be cancelled.
     */
    @Override
    public void onCancel(Appointment appt) {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Appointment")
                .setMessage("Are you sure you want to cancel this appointment?")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> {
                    appointmentController.cancelAppointment(appt.getId(), appt.getTimeslotId(), new AppointmentController.BookingCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(AppointmentHistoryActivity.this, "Appointment cancelled", Toast.LENGTH_SHORT).show();
                            loadHistory();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(AppointmentHistoryActivity.this, "Failed to cancel: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Implementation of HistoryAdapter.OnAppointmentInteractionListener.
     * Fetches all available slots to allow the user to choose a new time for rescheduling.
     * 
     * @param appt The appointment to be rescheduled.
     */
    @Override
    public void onReschedule(Appointment appt) {
        progressBar.setVisibility(View.VISIBLE);
        availabilityController.getAllAvailableSlots(new AvailabilityController.SlotListCallback() {
            @Override
            public void onSuccess(List<TimeSlot> slots) {
                progressBar.setVisibility(View.GONE);
                if (slots.isEmpty()) {
                    Toast.makeText(AppointmentHistoryActivity.this, "No other slots available", Toast.LENGTH_SHORT).show();
                    return;
                }
                showRescheduleDialog(appt, slots);
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AppointmentHistoryActivity.this, "Failed to load slots", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Displays a dialog with a list of available slots for the user to select from.
     * 
     * @param appt  The existing appointment being rescheduled.
     * @param slots List of currently available time slots.
     */
    private void showRescheduleDialog(Appointment appt, List<TimeSlot> slots) {
        String[] slotStrings = new String[slots.size()];
        for (int i = 0; i < slots.size(); i++) {
            TimeSlot s = slots.get(i);
            slotStrings[i] = s.getDate() + " " + s.getStartTime() + " (" + (s.getCounselorName() != null ? s.getCounselorName() : "Unknown") + ")";
        }

        new AlertDialog.Builder(this)
                .setTitle("Select New Time Slot")
                .setItems(slotStrings, (dialog, which) -> {
                    TimeSlot newSlot = slots.get(which);
                    performReschedule(appt, newSlot);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Calls the controller to update the appointment to a new time slot.
     * 
     * @param appt    The existing appointment record.
     * @param newSlot The new time slot selected by the student.
     */
    private void performReschedule(Appointment appt, TimeSlot newSlot) {
        // US-08: Passing 'this' (context) for notifications
        appointmentController.rescheduleAppointment(this, appt.getId(), appt.getTimeslotId(), newSlot, new AppointmentController.BookingCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(AppointmentHistoryActivity.this, "Rescheduled successfully", Toast.LENGTH_SHORT).show();
                loadHistory();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(AppointmentHistoryActivity.this, "Reschedule failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
