package com.example.counsellingapp.view;

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
import com.example.counsellingapp.model.Appointment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

/**
 * US-12 / US-14: Main landing screen for logged-in counselors.
 * Displays upcoming appointments (US-14) and provides navigation to
 * SetAvailabilityActivity (US-13).
 */
public class CounselorDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome, tvEmpty;
    private Button btnLogout, btnSetAvailability;
    private RecyclerView rvAppointments;
    private ProgressBar progressBar;// cool little ai find

    private AppointmentController appointmentController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counselor_dashboard);

        appointmentController = new AppointmentController();

        tvWelcome = findViewById(R.id.tvCounselorWelcome);
        tvEmpty = findViewById(R.id.tvNoAppointments);
        btnLogout = findViewById(R.id.btnCounselorLogout);
        btnSetAvailability = findViewById(R.id.btnSetAvailability);
        rvAppointments= findViewById(R.id.rvAppointments);
        progressBar= findViewById(R.id.progressBar);

        rvAppointments.setLayoutManager(new LinearLayoutManager(this));

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            tvWelcome.setText("Welcome, " + user.getEmail());
            loadAppointments(user.getUid());
        }

        btnSetAvailability.setOnClickListener(v ->
                startActivity(new Intent(this, SetAvailabilityActivity.class)));

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LandingActivity.class));
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the list every time the user returns from SetAvailabilityActivity
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) loadAppointments(user.getUid());
    }

    /**
     *Fetch and display upcoming appointments for this counselor.
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
                            rvAppointments.setAdapter(new AppointmentAdapter(appointments));
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
}