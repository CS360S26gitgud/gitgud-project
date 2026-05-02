package com.example.counsellingapp.view;

import android.graphics.Color;
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
 * RecyclerView adapter for the counselor management list inside {@link AdminDashboardActivity}.
 *
 * <p>Each card displays the counselor's name, email, specialization, approval state,
 * and suspension state, with "Edit", "Approve/Revoke", and (when suspended)
 * "Clear Suspension" action buttons.
 *
 * <p>CRC Responsibilities:
 * <ul>
 *   <li>Bind {@link Counselor} data to admin management cards.
 *   <li>Fire edit, approval-toggle, and suspension-clear callbacks to
 *       {@link AdminDashboardActivity}.
 * </ul>
 *
 * CRC Collaborators: {@link Counselor}, {@link AdminDashboardActivity}
 */
public class AdminCounselorAdapter
        extends RecyclerView.Adapter<AdminCounselorAdapter.ViewHolder> {

    /** Listener for edit actions on a counselor row. */
    public interface OnEditCounselorListener {
        void onEdit(Counselor counselor);
    }

    /** Listener for approve/revoke toggle actions on a counselor row. */
    public interface OnToggleApprovalListener {
        void onToggleApproval(Counselor counselor);
    }

    /** Listener for clearing a suspension on a suspended counselor row. */
    public interface OnClearSuspensionListener {
        void onClearSuspension(Counselor counselor);
    }

    private final List<Counselor>            counselors;
    private final OnEditCounselorListener    editListener;
    private final OnToggleApprovalListener   approvalListener;
    private final OnClearSuspensionListener  suspensionListener;

    /**
     * Constructs a new {@code AdminCounselorAdapter}.
     *
     * @param counselors         The list of {@link Counselor} objects to display.
     * @param editListener       Callback fired when the Edit button is tapped.
     * @param approvalListener   Callback fired when the Approve/Revoke button is tapped.
     * @param suspensionListener Callback fired when the Clear Suspension button is tapped.
     */
    public AdminCounselorAdapter(List<Counselor> counselors,
                                 OnEditCounselorListener editListener,
                                 OnToggleApprovalListener approvalListener,
                                 OnClearSuspensionListener suspensionListener) {
        this.counselors         = counselors;
        this.editListener       = editListener;
        this.approvalListener   = approvalListener;
        this.suspensionListener = suspensionListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_counselor, parent, false);
        return new ViewHolder(v);
    }

    /**
     * Binds a {@link Counselor} to a management card at the given position.
     *
     * <p>Status indicator logic:
     * <ul>
     *   <li>Suspended counselors show "Suspended" in red; the "Clear Suspension" button
     *       is visible only for this state.
     *   <li>Approved counselors show "Approved" in green; the toggle button reads "Revoke".
     *   <li>Unapproved counselors show "Pending" in grey; the toggle button reads "Approve".
     * </ul>
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Counselor counselor = counselors.get(position);

        holder.tvName.setText(counselor.getName() != null ? counselor.getName() : "—");
        holder.tvEmail.setText(counselor.getEmail() != null ? counselor.getEmail() : "—");

        String spec = (counselor.getSpecialization() != null
                && !counselor.getSpecialization().isEmpty())
                ? counselor.getSpecialization() : "General";
        holder.tvSpec.setText("Specialization: " + spec);

        String ratingText = counselor.getReviewCount() > 0
                ? String.format("%.1f / 5.0 (%d review%s)",
                        counselor.getAverageRating(),
                        counselor.getReviewCount(),
                        counselor.getReviewCount() == 1 ? "" : "s")
                : "No reviews yet";
        holder.tvRating.setText("Rating: " + ratingText);

        // Suspension takes visual priority over approval state
        if (counselor.isSuspended()) {
            holder.tvStatus.setText("Suspended");
            holder.tvStatus.setTextColor(Color.RED);
            holder.btnClearSuspension.setVisibility(View.VISIBLE);
        } else {
            holder.btnClearSuspension.setVisibility(View.GONE);
            if (counselor.isApproved()) {
                holder.tvStatus.setText("Approved");
                holder.tvStatus.setTextColor(Color.parseColor("#4CAF50"));
            } else {
                holder.tvStatus.setText("Pending Approval");
                holder.tvStatus.setTextColor(Color.GRAY);
            }
        }

        holder.btnToggleApproval.setText(counselor.isApproved() ? "Revoke" : "Approve");

        holder.btnEdit.setOnClickListener(v -> editListener.onEdit(counselor));
        holder.btnToggleApproval.setOnClickListener(v -> approvalListener.onToggleApproval(counselor));
        holder.btnClearSuspension.setOnClickListener(v -> suspensionListener.onClearSuspension(counselor));
    }

    @Override
    public int getItemCount() { return counselors.size(); }

    /** ViewHolder for a single counselor management card. */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvSpec, tvRating, tvStatus;
        Button   btnEdit, btnToggleApproval, btnClearSuspension;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName             = itemView.findViewById(R.id.tvAdminCounselorName);
            tvEmail            = itemView.findViewById(R.id.tvAdminCounselorEmail);
            tvSpec             = itemView.findViewById(R.id.tvAdminCounselorSpec);
            tvRating           = itemView.findViewById(R.id.tvAdminCounselorRating);
            tvStatus           = itemView.findViewById(R.id.tvAdminCounselorStatus);
            btnEdit            = itemView.findViewById(R.id.btnAdminEditCounselor);
            btnToggleApproval  = itemView.findViewById(R.id.btnAdminToggleCounselorApproval);
            btnClearSuspension = itemView.findViewById(R.id.btnAdminClearSuspension);
        }
    }
}
