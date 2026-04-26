package com.example.counsellingapp.view;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.counsellingapp.R;
import com.example.counsellingapp.model.User;

import java.util.List;

/**
 * RecyclerView adapter for the counselor search screen (US-10 + US-07 hook).
 * Each card shows the counselor's name, specialization, and available days,
 * plus a "View Reviews" button that opens CounselorReviewsActivity.
 */
public class CounselorAdapter extends RecyclerView.Adapter<CounselorAdapter.ViewHolder> {

    private final List<User> counselors;

    public CounselorAdapter(List<User> counselors) {
        this.counselors = counselors;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_counselor, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User counselor = counselors.get(position);
        Context ctx    = holder.itemView.getContext();

        String spec = (counselor.getSpecialization() != null
                && !counselor.getSpecialization().isEmpty())
                ? counselor.getSpecialization() : "General";

        String days = (counselor.getAvailableDays() != null
                && !counselor.getAvailableDays().isEmpty())
                ? String.join(", ", counselor.getAvailableDays()) : "Not set";

        holder.tvName.setText(counselor.getName() != null ? counselor.getName() : "—");
        holder.tvSpecialization.setText("Specialization: " + spec);
        holder.tvDays.setText("Available: " + days);

        holder.btnViewReviews.setOnClickListener(v -> {
            Intent intent = new Intent(ctx, CounselorReviewsActivity.class);
            intent.putExtra(CounselorReviewsActivity.EXTRA_COUNSELOR_ID,   counselor.getUid());
            intent.putExtra(CounselorReviewsActivity.EXTRA_COUNSELOR_NAME, counselor.getName());
            ctx.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return counselors.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvSpecialization, tvDays;
        Button   btnViewReviews;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName           = itemView.findViewById(R.id.tvCounselorCardName);
            tvSpecialization = itemView.findViewById(R.id.tvCounselorCardSpec);
            tvDays           = itemView.findViewById(R.id.tvCounselorCardDays);
            btnViewReviews   = itemView.findViewById(R.id.btnViewReviews);
        }
    }
}