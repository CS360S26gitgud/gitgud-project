package com.example.counsellingapp.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.counsellingapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Main landing screen for logged-in students.
 * Will host navigation to booking, search, and history features.
 */
public class StudentDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private Button btnLogout, btnViewSlots, btnSearchCounselors, btnAppointmentHistory;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        tvWelcome = findViewById(R.id.tvWelcome);
        btnLogout = findViewById(R.id.btnLogout);
        btnViewSlots = findViewById(R.id.btnViewSlots);
        btnSearchCounselors = findViewById(R.id.btnSearchCounselors);
        btnAppointmentHistory = findViewById(R.id.btnAppointmentHistory);

        // Show the logged in user's email
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            tvWelcome.setText("Welcome, " + user.getEmail());
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