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
 * RecyclerView adapter for the student's appointment history screen.
 * Supports User Story 11 (View History), User Story 06 (Reviews), and 
 * User Story 05 (Cancel and Reschedule).
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    /**
     * Listener interface for handling appointment management actions in the UI.
     */
    public interface OnAppointmentInteractionListener {
        /**
         * Triggered when the user clicks the Cancel button for an upcoming appointment.
         * @param appointment The appointment to be cancelled.
         */
        void onCancel(Appointment appointment);

        /**
         * Triggered when the user clicks the Reschedule button for an upcoming appointment.
         * @param appointment The appointment to be rescheduled.
         */
        void onReschedule(Appointment appointment);
    }

    private final List<Appointment> appointments;
    private final Set<String> reviewedIds;
    private final OnAppointmentInteractionListener listener;

    /**
     * Constructs a new HistoryAdapter.
     *
     * @param appointments List of appointments to display.
     * @param reviewedIds  Set of IDs for appointments that already have reviews.
     * @param listener     Listener for cancel and reschedule actions.
     */
    public HistoryAdapter(List<Appointment> appointments, Set<String> reviewedIds, OnAppointmentInteractionListener listener) {
        this.appointments = appointments;
        this.reviewedIds  = reviewedIds;
        this.listener     = listener;
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

        String counselorName = appt.getCounselorName() != null ? appt.getCounselorName() : 
                (appt.getCounselor() != null ? appt.getCounselor().getName() : "N/A");
                
        String dateStr = appt.getTimeSlot() != null
                ? appt.getTimeSlot().getDate() + " at " + appt.getTimeSlot().getStartTime()
                : "N/A";

        String status = appt.getStatus();

        holder.tvCounselorName.setText("Counselor: " + counselorName);
        holder.tvDateTime.setText("Date: " + dateStr);
        
        holder.tvStatus.setText("Status: " + status);

        // US-17: Show attached materials if any
        List<String> mats = appt.getMaterials();
        if (mats != null && !mats.isEmpty()) {
            holder.tvHistoryMaterials.setText("Materials:\n" + String.join("\n", mats));
            holder.tvHistoryMaterials.setVisibility(View.VISIBLE);
        } else {
            holder.tvHistoryMaterials.setVisibility(View.GONE);
        }


        // Reset visibility and colors to avoid state leakage in recycled views
        holder.btnLeaveReview.setVisibility(View.GONE);
        holder.tvReviewSubmitted.setVisibility(View.GONE);
        holder.btnCancelAppt.setVisibility(View.GONE);
        holder.btnRescheduleAppt.setVisibility(View.GONE);

        if ("rescheduled".equals(status)) {
            holder.tvStatus.setTextColor(android.graphics.Color.BLUE);
        } else if ("cancelled".equals(status)) {
            holder.tvStatus.setTextColor(android.graphics.Color.RED);
        } else if ("completed".equals(status)) {
            holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
            if (reviewedIds.contains(appt.getId())) {
                holder.tvReviewSubmitted.setVisibility(View.VISIBLE);
            } else {
                holder.btnLeaveReview.setVisibility(View.VISIBLE);
                holder.btnLeaveReview.setOnClickListener(v -> {
                    Intent intent = new Intent(ctx, SubmitReviewActivity.class);
                    intent.putExtra(SubmitReviewActivity.EXTRA_APPOINTMENT_ID, appt.getId());
                    intent.putExtra(SubmitReviewActivity.EXTRA_COUNSELOR_ID, appt.getCounselorId());
                    intent.putExtra(SubmitReviewActivity.EXTRA_COUNSELOR_NAME, counselorName);
                    ctx.startActivity(intent);
                });
            }
        } else if ("upcoming".equals(status)) {
            holder.tvStatus.setTextColor(android.graphics.Color.BLACK);
            holder.btnCancelAppt.setVisibility(View.VISIBLE);
            holder.btnRescheduleAppt.setVisibility(View.VISIBLE);
            
            holder.btnCancelAppt.setOnClickListener(v -> listener.onCancel(appt));
            holder.btnRescheduleAppt.setOnClickListener(v -> listener.onReschedule(appt));
        } else {
            holder.tvStatus.setTextColor(android.graphics.Color.BLACK);
        }
    }

    @Override
    public int getItemCount() { return appointments.size(); }

    /**
     * ViewHolder for appointment items in the history list.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCounselorName, tvDateTime, tvStatus, tvReviewSubmitted, tvHistoryMaterials;
        Button   btnLeaveReview, btnCancelAppt, btnRescheduleAppt;


        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCounselorName   = itemView.findViewById(R.id.tvHistoryCounselorName);
            tvDateTime        = itemView.findViewById(R.id.tvHistoryDateTime);
            tvStatus          = itemView.findViewById(R.id.tvHistoryStatus);
            tvReviewSubmitted = itemView.findViewById(R.id.tvReviewSubmitted);
            btnLeaveReview    = itemView.findViewById(R.id.btnLeaveReview);
            btnCancelAppt     = itemView.findViewById(R.id.btnCancelAppt);
            btnRescheduleAppt  = itemView.findViewById(R.id.btnRescheduleAppt);
            tvHistoryMaterials = itemView.findViewById(R.id.tvHistoryMaterials);

        }
    }
}
