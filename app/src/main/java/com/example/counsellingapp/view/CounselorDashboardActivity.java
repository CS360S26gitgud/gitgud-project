package com.example.counsellingapp.view;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.counsellingapp.R;
import com.example.counsellingapp.controller.AppointmentController;
import com.example.counsellingapp.controller.AvailabilityController;
import com.example.counsellingapp.model.Appointment;
import com.example.counsellingapp.model.TimeSlot;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;


import java.util.List;

/**
 * US-12 / US-14 / US-16: Main landing screen for logged-in counselors.
 * Provides access to upcoming appointments, availability settings, and reviews.
 * Supports counselor-initiated appointment management (cancellation and rescheduling).
 */
public class CounselorDashboardActivity extends BaseSessionActivity implements AppointmentAdapter.OnAppointmentActionListener {

    private TextView tvWelcome, tvEmpty;
    private Button btnLogout, btnSetAvailability;
    private Button btnMyReviews;
    private RecyclerView rvAppointments;
    private ProgressBar progressBar;

    private AppointmentController appointmentController;
    private AvailabilityController availabilityController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counselor_dashboard);

        appointmentController = new AppointmentController();
        availabilityController = new AvailabilityController();

        tvWelcome = findViewById(R.id.tvCounselorWelcome);
        tvEmpty = findViewById(R.id.tvNoAppointments);
        btnLogout = findViewById(R.id.btnCounselorLogout);
        btnSetAvailability = findViewById(R.id.btnSetAvailability);
        btnMyReviews = findViewById(R.id.btnMyReviews);
        rvAppointments= findViewById(R.id.rvAppointments);
        progressBar= findViewById(R.id.progressBar);

        rvAppointments.setLayoutManager(new LinearLayoutManager(this));

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Fetch name from Firestore
            FirebaseFirestore.getInstance().collection("counselors")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        tvWelcome.setText("Welcome, " + (name != null ? name : user.getEmail()));
                    } else {
                        tvWelcome.setText("Welcome, " + user.getEmail());
                    }
                })
                .addOnFailureListener(e -> tvWelcome.setText("Welcome!"));

            loadAppointments(user.getUid());
        }


        btnSetAvailability.setOnClickListener(v ->
                startActivity(new Intent(this, SetAvailabilityActivity.class)));

        btnMyReviews.setOnClickListener(v -> {
            FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
            if (current != null) {
                Intent intent = new Intent(this, CounselorReviewsActivity.class);
                intent.putExtra(CounselorReviewsActivity.EXTRA_COUNSELOR_ID, current.getUid());
                intent.putExtra(CounselorReviewsActivity.EXTRA_COUNSELOR_NAME, current.getEmail());
                startActivity(intent);
            }
        });

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LandingActivity.class));
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) loadAppointments(user.getUid());
    }

    /**
     * Fetches and displays upcoming appointments for the counselor from Firestore.
     * @param counselorId The unique identifier of the counselor.
     */
    private void loadAppointments(String counselorId) {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        appointmentController.getUpcomingForCounselor(counselorId,
                new AppointmentController.AppointmentListCallback() {
                    @Override
                    public void onSuccess(List<Appointment> appointments) {
                        progressBar.setVisibility(View.GONE);
                        if (appointments.isEmpty()) {
                            tvEmpty.setVisibility(View.VISIBLE);
                            rvAppointments.setVisibility(View.GONE);
                        } else {
                            tvEmpty.setVisibility(View.GONE);
                            rvAppointments.setVisibility(View.VISIBLE);
                            rvAppointments.setAdapter(new AppointmentAdapter(appointments, CounselorDashboardActivity.this));
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(CounselorDashboardActivity.this,
                                "Could not load appointments: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Triggered when the counselor clicks the Cancel button.
     * Shows a confirmation dialog and invokes the controller to cancel the appointment.
     * @param appointment The appointment to be cancelled.
     */
    @Override
    public void onCancel(Appointment appointment) {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Appointment")
                .setMessage("Are you sure you want to cancel this appointment with " + appointment.getStudent().getName() + "?")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> {
                    appointmentController.cancelAppointmentByCounselor(this, appointment, new AppointmentController.BookingCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(CounselorDashboardActivity.this, "Appointment cancelled", Toast.LENGTH_SHORT).show();
                            loadAppointments(FirebaseAuth.getInstance().getUid());
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(CounselorDashboardActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Triggered when the counselor clicks the Reschedule button.
     * Fetches available slots for the counselor and displays them in a selection dialog.
     * @param appointment The appointment to be rescheduled.
     */
    @Override
    public void onReschedule(Appointment appointment) {
        progressBar.setVisibility(View.VISIBLE);
        availabilityController.getCounselorSlots(appointment.getCounselorId(), new AvailabilityController.SlotListCallback() {
            @Override
            public void onSuccess(List<TimeSlot> slots) {
                progressBar.setVisibility(View.GONE);
                if (slots.isEmpty()) {
                    Toast.makeText(CounselorDashboardActivity.this, "No other slots available", Toast.LENGTH_SHORT).show();
                    return;
                }
                showRescheduleDialog(appointment, slots);
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(CounselorDashboardActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Displays a dialog listing available time slots for rescheduling.
     * @param appt  The appointment to reschedule.
     * @param slots The list of available time slots.
     */
    private void showRescheduleDialog(Appointment appt, List<TimeSlot> slots) {
        String[] slotStrings = new String[slots.size()];
        for (int i = 0; i < slots.size(); i++) {
            TimeSlot s = slots.get(i);
            slotStrings[i] = s.getDate() + " " + s.getStartTime();
        }

        new AlertDialog.Builder(this)
                .setTitle("Select New Slot for " + appt.getStudent().getName())
                .setItems(slotStrings, (dialog, which) -> {
                    TimeSlot newSlot = slots.get(which);
                    appointmentController.rescheduleAppointmentByCounselor(this, appt, newSlot, new AppointmentController.BookingCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(CounselorDashboardActivity.this, "Rescheduled successfully", Toast.LENGTH_SHORT).show();
                            loadAppointments(FirebaseAuth.getInstance().getUid());
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(CounselorDashboardActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Triggered when the counselor clicks the Complete button.
     * Invokes the controller to mark the appointment as completed.
     * @param appointment The appointment to be marked as completed.
     */
    @Override
    public void onComplete(Appointment appointment) {
        appointmentController.markAsCompleted(appointment.getId(), new AppointmentController.BookingCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(CounselorDashboardActivity.this, "Marked as completed", Toast.LENGTH_SHORT).show();
                loadAppointments(FirebaseAuth.getInstance().getUid());
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(CounselorDashboardActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}