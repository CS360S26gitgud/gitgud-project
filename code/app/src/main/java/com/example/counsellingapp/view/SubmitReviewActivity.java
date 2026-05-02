package com.example.counsellingapp.view;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.counsellingapp.R;
import com.example.counsellingapp.controller.ReviewController;
import com.example.counsellingapp.model.Review;
import com.google.firebase.Timestamp;

/**
 * US-06: Allows a student to anonymously rate and comment on a completed appointment.
 *
 * Entry point : HistoryAdapter — "Leave a Review" button on a completed appointment card.
 * On success  : finishes and returns to AppointmentHistoryActivity, which refreshes
 *               onResume() and flips the button to "✓ Review submitted".
 *
 * Intent extras (all required):
 *   EXTRA_APPOINTMENT_ID  — becomes the Firestore document ID for this review
 *   EXTRA_COUNSELOR_ID    — stored in the review for counselor-query support
 *   EXTRA_COUNSELOR_NAME  — display only, shown in the screen title
 */
public class SubmitReviewActivity extends AppCompatActivity {

    public static final String EXTRA_APPOINTMENT_ID = "appointmentId";
    public static final String EXTRA_COUNSELOR_ID   = "counselorId";
    public static final String EXTRA_COUNSELOR_NAME = "counselorName";

    private RatingBar   ratingBar;
    private EditText    etComment;
    private Button      btnSubmit;
    private ProgressBar progressBar;
    private TextView    tvTitle;

    private ReviewController reviewController;
    private String appointmentId;
    private String counselorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit_review);

        reviewController = new ReviewController();

        appointmentId        = getIntent().getStringExtra(EXTRA_APPOINTMENT_ID);
        counselorId          = getIntent().getStringExtra(EXTRA_COUNSELOR_ID);
        String counselorName = getIntent().getStringExtra(EXTRA_COUNSELOR_NAME);

        ratingBar   = findViewById(R.id.ratingBar);
        etComment   = findViewById(R.id.etComment);
        btnSubmit   = findViewById(R.id.btnSubmitReview);
        progressBar = findViewById(R.id.progressBarReview);
        tvTitle     = findViewById(R.id.tvReviewTitle);

        if (counselorName != null && !counselorName.isEmpty()) {
            tvTitle.setText("Review session with " + counselorName);
        }

        btnSubmit.setOnClickListener(v -> handleSubmit());
    }

    private void handleSubmit() {
        float rating = ratingBar.getRating();
        if (rating == 0f) {
            Toast.makeText(this, "Please select a star rating before submitting.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String comment = etComment.getText().toString().trim();

        progressBar.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        Review review = new Review(appointmentId, counselorId, rating, comment, Timestamp.now());

        reviewController.submitReview(review, new ReviewController.SimpleCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(SubmitReviewActivity.this,
                        "Review submitted — thank you!", Toast.LENGTH_SHORT).show();
                finish(); // Returns to AppointmentHistoryActivity which refreshes onResume()
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                btnSubmit.setEnabled(true);
                Toast.makeText(SubmitReviewActivity.this,
                        "Failed to submit: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}