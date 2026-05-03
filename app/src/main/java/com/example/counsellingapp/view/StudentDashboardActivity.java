package com.example.counsellingapp.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.counsellingapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.widget.CalendarView;
import com.example.counsellingapp.controller.AppointmentController;
import com.example.counsellingapp.model.Appointment;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;


/**
 * Main landing screen for logged-in students.
 * Surfaces key navigation to search, booking, and history.
 *
 * <p><b>US-09 — Calendar Integration:</b>
 * On launch, the dashboard highlights the student's next upcoming appointment
 * on the integrated {@code CalendarView} for passive tracking.
 */
public class StudentDashboardActivity extends AppCompatActivity {


    private TextView tvWelcome;
    private Button btnLogout, btnViewSlots, btnSearchCounselors, btnAppointmentHistory;
    private CalendarView calendarView;
    private AppointmentController appointmentController;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        tvWelcome = findViewById(R.id.tvWelcome);
        btnLogout = findViewById(R.id.btnLogout);
        btnViewSlots = findViewById(R.id.btnViewSlots);
        btnSearchCounselors = findViewById(R.id.btnSearchCounselors);
        btnAppointmentHistory = findViewById(R.id.btnAppointmentHistory);
        calendarView = findViewById(R.id.calendarView);
        appointmentController = new AppointmentController();


        // Show the logged in user's email
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            tvWelcome.setText("Welcome, " + user.getEmail());
        }
        // US-09: Highlight the student's next upcoming appointment on the calendar
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            appointmentController.getStudentUpcomingAppointments(uid,
                    new AppointmentController.AppointmentListCallback() {
                        @Override
                        public void onSuccess(List<Appointment> appointments) {
                            if (!appointments.isEmpty() && appointments.get(0).getTimeSlot() != null) {
                                String date = appointments.get(0).getTimeSlot().getDate();
                                try {
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                    calendarView.setDate(sdf.parse(date).getTime(), true, true);
                                } catch (Exception ignored) {}
                            }
                        }
                        @Override
                        public void onFailure(Exception e) {}
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