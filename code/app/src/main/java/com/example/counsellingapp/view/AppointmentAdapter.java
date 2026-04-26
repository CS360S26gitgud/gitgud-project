package com.example.counsellingapp.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.counsellingapp.R;
import com.example.counsellingapp.controller.AppointmentController;
import com.example.counsellingapp.model.Appointment;

import java.util.List;

/**
 * RecyclerView adapter for displaying Appointment objects in the counselor dashboard.
 *
 * Updated to support marking appointments as completed (US-06 enabler).
 * "Mark as Completed" button is only shown on upcoming appointments.
 * Once tapped, the status updates in Firestore and the button is replaced
 * by the updated status text immediately in the UI without a full reload.
 */
public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.ViewHolder> {

    private final List<Appointment> appointments;
    private final AppointmentController appointmentController;

    public AppointmentAdapter(List<Appointment> appointments) {
        this.appointments          = appointments;
        this.appointmentController = new AppointmentController();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appointment, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Appointment appt = appointments.get(position);

        String studentName = appt.getStudent() != null
                ? appt.getStudent().getName() : "Unknown student";
        String date      = appt.getTimeSlot() != null ? appt.getTimeSlot().getDate()      : "—";
        String startTime = appt.getTimeSlot() != null ? appt.getTimeSlot().getStartTime() : "—";
        String endTime   = appt.getTimeSlot() != null ? appt.getTimeSlot().getEndTime()   : "—";

        holder.tvStudentName.setText("Student: " + studentName);
        holder.tvDate.setText("Date: " + date);
        holder.tvTime.setText("Time: " + startTime + " – " + endTime);
        holder.tvStatus.setText("Status: " + appt.getStatus());

        // Only show the button on upcoming appointments
        if ("upcoming".equals(appt.getStatus())) {
            holder.btnMarkCompleted.setVisibility(View.VISIBLE);
            holder.btnMarkCompleted.setOnClickListener(v -> {
                // Disable immediately to prevent double-taps
                holder.btnMarkCompleted.setEnabled(false);

                appointmentController.markAsCompleted(appt.getId(),
                        new AppointmentController.BookingCallback() {
                            @Override
                            public void onSuccess() {
                                // Update the in-memory object and rebind this item —
                                // no full list reload needed
                                appt.setStatus("completed");
                                notifyItemChanged(position);
                            }

                            @Override
                            public void onFailure(Exception e) {
                                holder.btnMarkCompleted.setEnabled(true);
                                Toast.makeText(holder.itemView.getContext(),
                                        "Failed to update: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            });
        } else {
            // Completed or cancelled — hide the button
            holder.btnMarkCompleted.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() { return appointments.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName, tvDate, tvTime, tvStatus;
        Button   btnMarkCompleted;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName    = itemView.findViewById(R.id.tvStudentName);
            tvDate           = itemView.findViewById(R.id.tvDate);
            tvTime           = itemView.findViewById(R.id.tvTime);
            tvStatus         = itemView.findViewById(R.id.tvStatus);
            btnMarkCompleted = itemView.findViewById(R.id.btnMarkCompleted);
        }
    }
}