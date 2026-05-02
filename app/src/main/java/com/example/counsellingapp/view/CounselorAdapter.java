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
import com.example.counsellingapp.model.Counselor;

import java.util.List;

/**
 * RecyclerView adapter for the counselor search screen.
 *
 * <p>Updated to accept {@code List<Counselor>} instead of the former {@code List<User>}.
 * Because {@link Counselor} is the only type that carries {@code specialization} and
 * {@code availableDays}, the previous null-guards for those fields as <em>type-mismatch
 * protection</em> are no longer needed. Null checks for the field values themselves
 * (e.g. a counselor who has not yet set a specialization) are still present since those
 * represent optional data.
 *
 * <p>Every counselor shown by this adapter is already guaranteed to be approved and
 * not suspended, because {@code CounselorController} applies that filter before
 * delivering results. This adapter does not need to re-check those flags.
 *
 * <p>CRC Responsibilities:
 * <ul>
 *   <li>Bind {@link Counselor} data to item cards in the search results list.
 *   <li>Launch {@link CounselorReviewsActivity} with the selected counselor's ID and name.
 * </ul>
 *
 * CRC Collaborators: {@link Counselor}, {@link CounselorReviewsActivity},
 *                    {@link CounselorSearchActivity}
 */
public class CounselorAdapter extends RecyclerView.Adapter<CounselorAdapter.ViewHolder> {

    private final List<Counselor> counselors;

    /**
     * Constructs a new {@code CounselorAdapter}.
     *
     * @param counselors The list of approved, non-suspended {@link Counselor} objects
     *                   to display. Must not be {@code null}.
     */
    public CounselorAdapter(List<Counselor> counselors) {
        this.counselors = counselors;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_counselor, parent, false);
        return new ViewHolder(v);
    }

    /**
     * Binds a {@link Counselor} to a card view at the given position.
     *
     * <p>Specialization defaults to {@code "General"} if {@code null} or empty — that
     * label is only used in the UI and is never stored in Firestore.
     * Available days defaults to {@code "Not set"} if {@code null} or empty.
     *
     * @param holder   The {@link ViewHolder} to populate.
     * @param position Zero-based position of the item in the adapter list.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Counselor counselor = counselors.get(position);
        Context   ctx       = holder.itemView.getContext();

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

    /** @return The number of counselors in the adapter list. */
    @Override
    public int getItemCount() { return counselors.size(); }

    /**
     * ViewHolder for a single counselor card in the search results list.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvSpecialization, tvDays;
        Button   btnViewReviews;

        /**
         * Constructs a new {@code ViewHolder} and binds its child views.
         *
         * @param itemView The inflated item view for a single counselor card.
         */
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName           = itemView.findViewById(R.id.tvCounselorCardName);
            tvSpecialization = itemView.findViewById(R.id.tvCounselorCardSpec);
            tvDays           = itemView.findViewById(R.id.tvCounselorCardDays);
            btnViewReviews   = itemView.findViewById(R.id.btnViewReviews);
        }
    }
}