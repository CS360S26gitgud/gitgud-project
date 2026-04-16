package com.example.counsellingapp.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.counsellingapp.R;
import com.example.counsellingapp.model.Appointment;

import java.util.List;

/**
 * RecyclerView adapter for displaying Appointment objects in the counselor dashboard.
 * Reads data through resolved collaborator objects (Student, TimeSlot) as per CRC design.
 */
public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.ViewHolder> {

    private final List<Appointment> appointments;

    public AppointmentAdapter(List<Appointment> appointments) {
        this.appointments = appointments;
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

        // Access data through resolved collaborator objects — CRC-compliant
        String studentName = appt.getStudent() != null
                ? appt.getStudent().getName() : "Unknown student";

        String date      = appt.getTimeSlot() != null ? appt.getTimeSlot().getDate()      : "—";
        String startTime = appt.getTimeSlot() != null ? appt.getTimeSlot().getStartTime() : "—";
        String endTime   = appt.getTimeSlot() != null ? appt.getTimeSlot().getEndTime()   : "—";

        holder.tvStudentName.setText("Student: " + studentName);
        holder.tvDate.setText("Date: " + date);
        holder.tvTime.setText("Time: " + startTime + " – " + endTime);
        holder.tvStatus.setText("Status: " + appt.getStatus());
    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName, tvDate, tvTime, tvStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvDate        = itemView.findViewById(R.id.tvDate);
            tvTime        = itemView.findViewById(R.id.tvTime);
            tvStatus      = itemView.findViewById(R.id.tvStatus);
        }
    }
}