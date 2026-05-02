package com.example.counsellingapp.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.counsellingapp.R;
import com.example.counsellingapp.model.Review;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for rendering individual Review items inside CounselorReviewsActivity.
 * Displays a read-only RatingBar, the written comment, and the submission date.
 */
public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {

    private final List<Review> reviews;

    public ReviewAdapter(List<Review> reviews) {
        this.reviews = reviews;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Review review = reviews.get(position);

        holder.ratingBar.setRating(review.getRating());

        String comment = (review.getComment() != null && !review.getComment().isEmpty())
                ? review.getComment() : "(No written comment)";
        holder.tvComment.setText(comment);

        if (review.getTimestamp() != null) {
            Date date = review.getTimestamp().toDate();
            holder.tvDate.setText(
                    new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date));
        } else {
            holder.tvDate.setText("");
        }
    }

    @Override
    public int getItemCount() { return reviews.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        RatingBar ratingBar;
        TextView  tvComment, tvDate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ratingBar = itemView.findViewById(R.id.ratingBarDisplay);
            tvComment = itemView.findViewById(R.id.tvReviewComment);
            tvDate    = itemView.findViewById(R.id.tvReviewDate);
        }
    }
}