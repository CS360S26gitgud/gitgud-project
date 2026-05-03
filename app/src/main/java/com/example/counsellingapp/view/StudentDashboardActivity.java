package com.example.counsellingapp.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.TextView;

import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.counsellingapp.R;
import com.example.counsellingapp.controller.AppointmentController;
import com.example.counsellingapp.model.Appointment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;


import java.util.List;

/**
 * Main landing screen for logged-in students.
 * Surfaces key navigation to search, booking, and history.
 */
public class StudentDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome, tvNextApptDetails;
    private CalendarView calendarView;
    private Button btnLogout, btnViewSlots, btnSearchCounselors, btnAppointmentHistory;

    private AppointmentController appointmentController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        tvWelcome = findViewById(R.id.tvWelcome);
        tvNextApptDetails = findViewById(R.id.tvNextApptDetails);
        calendarView = findViewById(R.id.calendarView);
        btnLogout = findViewById(R.id.btnLogout);

        btnViewSlots = findViewById(R.id.btnViewSlots);
        btnSearchCounselors = findViewById(R.id.btnSearchCounselors);
        btnAppointmentHistory = findViewById(R.id.btnAppointmentHistory);
        
        appointmentController = new AppointmentController();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Initially show a generic welcome to avoid showing the email
            tvWelcome.setText("Welcome!");

            // Fetch the full name from the 'students' Firestore collection
            FirebaseFirestore.getInstance().collection("students")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.contains("name")) {
                        String fullName = doc.getString("name");
                        if (fullName != null && !fullName.isEmpty()) {
                            tvWelcome.setText("Welcome, " + fullName);
                        } else {
                            tvWelcome.setText("Welcome, " + user.getEmail());
                        }
                    } else {
                        // Fallback to email only if name is missing from DB
                        tvWelcome.setText("Welcome, " + user.getEmail());
                    }
                })
                .addOnFailureListener(e -> {
                    tvWelcome.setText("Welcome, " + user.getEmail());
                });
        }



        // US-09: Show the student's next upcoming appointment details
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            appointmentController.getStudentUpcomingAppointments(uid,
                    new AppointmentController.AppointmentListCallback() {
                        @Override
                        public void onSuccess(List<Appointment> appointments) {
                            if (!appointments.isEmpty() && appointments.get(0).getTimeSlot() != null) {
                                String date = appointments.get(0).getTimeSlot().getDate();
                                String time = appointments.get(0).getTimeSlot().getStartTime();
                                String counselor = appointments.get(0).getCounselorName();
                                
                                String details = date + " at " + time;
                                if (counselor != null && !counselor.isEmpty()) {
                                    details += "\nwith " + counselor;
                                }
                                tvNextApptDetails.setText(details);

                                // US-09: Highlight/Focus the calendar on this date
                                try {
                                    long dateMillis = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                        .parse(date).getTime();
                                    calendarView.setDate(dateMillis, true, true);
                                } catch (Exception e) {
                                }
                            } else {
                                tvNextApptDetails.setText("No upcoming sessions.");
                            }
                        }


                        @Override
                        public void onFailure(Exception e) {
                            tvNextApptDetails.setText("Could not load next session.");
                        }
                    });
        }

        btnViewSlots.setOnClickListener(v -> {
            startActivity(new Intent(this, AvailableSlotsActivity.class));
        });

        btnSearchCounselors.setOnClickListener(v -> {
            startActivity(new Intent(this, CounselorSearchActivity.class));
        });

        btnAppointmentHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, AppointmentHistoryActivity.class));
        });

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}