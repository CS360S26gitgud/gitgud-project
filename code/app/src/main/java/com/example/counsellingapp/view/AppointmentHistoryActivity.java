package com.example.counsellingapp.view;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.counsellingapp.R;
import com.example.counsellingapp.controller.AppointmentController;
import com.example.counsellingapp.controller.ReviewController;
import com.example.counsellingapp.model.Appointment;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * US-11 (existing): Student views appointment history.
 * US-06 (new hook): "Leave a Review" button appears on completed, un-reviewed appointments.
 *
 * Upgraded from ListView + ArrayAdapter<String> to RecyclerView + HistoryAdapter.
 * onResume() reloads the list so the review button state is always fresh after
 * returning from SubmitReviewActivity.
 */
public class AppointmentHistoryActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private ProgressBar  progressBar;
    private TextView     tvEmpty;

    private AppointmentController appointmentController;
    private ReviewController      reviewController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_history);

        appointmentController = new AppointmentController();
        reviewController      = new ReviewController();

        rvHistory   = findViewById(R.id.rvHistory);
        progressBar = findViewById(R.id.progressBarHistory);
        tvEmpty     = findViewById(R.id.tvNoHistory);

        rvHistory.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload every time we return — covers the case where the student just
        // submitted a review and the button state needs to flip.
        loadHistory();
    }

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

                        // Collect IDs of completed appointments — only these can have reviews
                        List<String> completedIds = appointments.stream()
                                .filter(a -> "completed".equals(a.getStatus()))
                                .map(Appointment::getId)
                                .collect(Collectors.toList());

                        if (completedIds.isEmpty()) {
                            // No completed appointments — skip review check entirely
                            progressBar.setVisibility(View.GONE);
                            renderList(appointments, new HashSet<>());
                            return;
                        }

                        // One Firestore read to find which completed appointments are reviewed
                        reviewController.getReviewedAppointmentIds(completedIds,
                                new ReviewController.ReviewedIdsCallback() {
                                    @Override
                                    public void onSuccess(Set<String> reviewedIds) {
                                        progressBar.setVisibility(View.GONE);
                                        renderList(appointments, reviewedIds);
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        // Non-fatal: render list anyway, all buttons visible
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

    private void renderList(List<Appointment> appointments, Set<String> reviewedIds) {
        rvHistory.setVisibility(View.VISIBLE);
        rvHistory.setAdapter(new HistoryAdapter(appointments, reviewedIds));
    }
}