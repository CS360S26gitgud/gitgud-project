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
 * Supports marking as completed, cancelling, and rescheduling.
 */
public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.ViewHolder> {

    public interface OnAppointmentActionListener {
        void onCancel(Appointment appointment);
        void onReschedule(Appointment appointment);
        void onComplete(Appointment appointment);
    }

    private final List<Appointment> appointments;
    private final OnAppointmentActionListener listener;

    public AppointmentAdapter(List<Appointment> appointments, OnAppointmentActionListener listener) {
        this.appointments = appointments;
        this.listener     = listener;
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

        // Reset visibility
        holder.btnMarkCompleted.setVisibility(View.GONE);
        holder.btnCancel.setVisibility(View.GONE);
        holder.btnReschedule.setVisibility(View.GONE);

        if ("upcoming".equals(appt.getStatus())) {
            holder.btnMarkCompleted.setVisibility(View.VISIBLE);
            holder.btnCancel.setVisibility(View.VISIBLE);
            holder.btnReschedule.setVisibility(View.VISIBLE);

            holder.btnMarkCompleted.setOnClickListener(v -> listener.onComplete(appt));
            holder.btnCancel.setOnClickListener(v -> listener.onCancel(appt));
            holder.btnReschedule.setOnClickListener(v -> listener.onReschedule(appt));
        }
    }

    @Override
    public int getItemCount() { return appointments.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName, tvDate, tvTime, tvStatus;
        Button   btnMarkCompleted, btnCancel, btnReschedule;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName    = itemView.findViewById(R.id.tvStudentName);
            tvDate           = itemView.findViewById(R.id.tvDate);
            tvTime           = itemView.findViewById(R.id.tvTime);
            tvStatus         = itemView.findViewById(R.id.tvStatus);
            btnMarkCompleted = itemView.findViewById(R.id.btnMarkCompleted);
            btnCancel        = itemView.findViewById(R.id.btnCancelByCounselor);
            btnReschedule    = itemView.findViewById(R.id.btnRescheduleByCounselor);
        }
    }
}