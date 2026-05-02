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
import com.example.counsellingapp.controller.ReviewController;
import com.example.counsellingapp.model.Review;

import java.util.List;
import java.util.Locale;

/**
 * US-07: Student views a counselor's reviews — launched from CounselorAdapter.
 * US-15: Counselor views their own reviews — launched from CounselorDashboardActivity.
 *
 * A single Activity serves both user stories. The only difference is who launches it
 * and with whose counselorId. The screen is completely read-only.
 *
 * Required Intent extras:
 *   EXTRA_COUNSELOR_ID   — UID used to query the "reviews" collection
 *   EXTRA_COUNSELOR_NAME — display name shown in the screen title
 */
public class CounselorReviewsActivity extends AppCompatActivity {

    public static final String EXTRA_COUNSELOR_ID   = "counselorId";
    public static final String EXTRA_COUNSELOR_NAME = "counselorName";

    private TextView    tvTitle, tvAverageRating, tvNoReviews;
    private RecyclerView rvReviews;
    private ProgressBar  progressBar;

    private ReviewController reviewController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counselor_reviews);

        reviewController = new ReviewController();

        String counselorId   = getIntent().getStringExtra(EXTRA_COUNSELOR_ID);
        String counselorName = getIntent().getStringExtra(EXTRA_COUNSELOR_NAME);

        tvTitle         = findViewById(R.id.tvReviewsTitle);
        tvAverageRating = findViewById(R.id.tvAverageRating);
        tvNoReviews     = findViewById(R.id.tvNoReviews);
        rvReviews       = findViewById(R.id.rvReviews);
        progressBar     = findViewById(R.id.progressBarReviews);

        rvReviews.setLayoutManager(new LinearLayoutManager(this));

        tvTitle.setText(counselorName != null ? counselorName + "'s Reviews" : "Reviews");

        if (counselorId != null) {
            loadReviews(counselorId);
        }
    }

    private void loadReviews(String counselorId) {
        progressBar.setVisibility(View.VISIBLE);

        reviewController.getReviewsForCounselor(counselorId, new ReviewController.ReviewListCallback() {
            @Override
            public void onSuccess(List<Review> reviews) {
                progressBar.setVisibility(View.GONE);

                if (reviews.isEmpty()) {
                    tvNoReviews.setVisibility(View.VISIBLE);
                    tvAverageRating.setVisibility(View.GONE);
                    rvReviews.setVisibility(View.GONE);
                } else {
                    tvNoReviews.setVisibility(View.GONE);
                    tvAverageRating.setVisibility(View.VISIBLE);
                    rvReviews.setVisibility(View.VISIBLE);

                    float total = 0;
                    for (Review r : reviews) total += r.getRating();
                    float avg = total / reviews.size();

                    tvAverageRating.setText(String.format(Locale.getDefault(),
                            "Average Rating: %.1f / 5.0  (%d review%s)",
                            avg, reviews.size(), reviews.size() == 1 ? "" : "s"));

                    rvReviews.setAdapter(new ReviewAdapter(reviews));
                }
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(CounselorReviewsActivity.this,
                        "Could not load reviews: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}