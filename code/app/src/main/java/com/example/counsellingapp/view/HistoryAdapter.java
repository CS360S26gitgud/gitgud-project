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
import com.example.counsellingapp.model.Appointment;

import java.util.List;
import java.util.Set;

/**
 * RecyclerView adapter for the student's appointment history screen (US-11 + US-06 hook).
 *
 * Per-item review state logic:
 *   - status == "completed" AND id NOT in reviewedIds → show "Leave a Review" button
 *   - status == "completed" AND id IN reviewedIds     → show "✓ Review submitted" label
 *   - status == "upcoming"  OR  "cancelled"           → neither element shown
 *
 * The reviewedIds Set is computed once in AppointmentHistoryActivity (one Firestore read)
 * and passed here at construction — no async work happens at bind time.
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final List<Appointment> appointments;
    private final Set<String> reviewedIds;

    public HistoryAdapter(List<Appointment> appointments, Set<String> reviewedIds) {
        this.appointments = appointments;
        this.reviewedIds  = reviewedIds;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history_appointment, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Appointment appt = appointments.get(position);
        Context ctx = holder.itemView.getContext();

        String counselorName = appt.getCounselor() != null
                ? appt.getCounselor().getName() : "N/A";
        String dateStr = appt.getTimeSlot() != null
                ? appt.getTimeSlot().getDate() + " at " + appt.getTimeSlot().getStartTime()
                : "N/A";

        holder.tvCounselorName.setText("Counselor: " + counselorName);
        holder.tvDateTime.setText("Date: " + dateStr);
        holder.tvStatus.setText("Status: " + appt.getStatus());

        if ("completed".equals(appt.getStatus())) {
            if (reviewedIds.contains(appt.getId())) {
                // Review already submitted — show confirmation, hide button
                holder.btnLeaveReview.setVisibility(View.GONE);
                holder.tvReviewSubmitted.setVisibility(View.VISIBLE);
            } else {
                // Eligible for review — show button
                holder.btnLeaveReview.setVisibility(View.VISIBLE);
                holder.tvReviewSubmitted.setVisibility(View.GONE);
                holder.btnLeaveReview.setOnClickListener(v -> {
                    Intent intent = new Intent(ctx, SubmitReviewActivity.class);
                    intent.putExtra(SubmitReviewActivity.EXTRA_APPOINTMENT_ID, appt.getId());
                    intent.putExtra(SubmitReviewActivity.EXTRA_COUNSELOR_ID, appt.getCounselorId());
                    intent.putExtra(SubmitReviewActivity.EXTRA_COUNSELOR_NAME,
                            appt.getCounselor() != null ? appt.getCounselor().getName() : "");
                    ctx.startActivity(intent);
                });
            }
        } else {
            // Upcoming or cancelled — no review UI
            holder.btnLeaveReview.setVisibility(View.GONE);
            holder.tvReviewSubmitted.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() { return appointments.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCounselorName, tvDateTime, tvStatus, tvReviewSubmitted;
        Button   btnLeaveReview;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCounselorName   = itemView.findViewById(R.id.tvHistoryCounselorName);
            tvDateTime        = itemView.findViewById(R.id.tvHistoryDateTime);
            tvStatus          = itemView.findViewById(R.id.tvHistoryStatus);
            tvReviewSubmitted = itemView.findViewById(R.id.tvReviewSubmitted);
            btnLeaveReview    = itemView.findViewById(R.id.btnLeaveReview);
        }
    }
}