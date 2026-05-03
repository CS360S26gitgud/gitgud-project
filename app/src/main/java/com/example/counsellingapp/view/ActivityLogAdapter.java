package com.example.counsellingapp.view;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.counsellingapp.R;
import com.example.counsellingapp.model.SystemActivity;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying system activity logs in the Admin Dashboard (US-21).
 * Handles color-coding of logs based on event type (e.g., Green for approvals,
 * Red for cancellations) to provide a premium monitoring experience.
 */
public class ActivityLogAdapter extends RecyclerView.Adapter<ActivityLogAdapter.ViewHolder> {


    private final List<SystemActivity> activities;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());

    public ActivityLogAdapter(List<SystemActivity> activities) {
        this.activities = activities;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_system_activity, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SystemActivity activity = activities.get(position);

        holder.tvDescription.setText(activity.getDescription());
        holder.tvInitiator.setText("Initiated by: " + activity.getInitiatorName());
        holder.tvType.setText(activity.getType());
        
        if (activity.getTimestamp() != null) {
            holder.tvTimestamp.setText(dateFormat.format(activity.getTimestamp().toDate()));
        }

        // Color coding based on type
        switch (activity.getType()) {
            case "BOOKING":
            case "APPROVAL":
                holder.tvType.setBackgroundColor(Color.parseColor("#E8F5E9"));
                holder.tvType.setTextColor(Color.parseColor("#2E7D32"));
                break;
            case "CANCELLATION":
            case "DEACTIVATION":
                holder.tvType.setBackgroundColor(Color.parseColor("#FFEBEE"));
                holder.tvType.setTextColor(Color.parseColor("#C62828"));
                break;
            case "REVIEW":
                holder.tvType.setBackgroundColor(Color.parseColor("#E3F2FD"));
                holder.tvType.setTextColor(Color.parseColor("#1565C0"));
                break;
            default:
                holder.tvType.setBackgroundColor(Color.parseColor("#EEEEEE"));
                holder.tvType.setTextColor(Color.parseColor("#424242"));
        }
    }

    @Override
    public int getItemCount() {
        return activities.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvTimestamp, tvDescription, tvInitiator;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvType        = itemView.findViewById(R.id.tvLogType);
            tvTimestamp   = itemView.findViewById(R.id.tvLogTimestamp);
            tvDescription = itemView.findViewById(R.id.tvLogDescription);
            tvInitiator   = itemView.findViewById(R.id.tvLogInitiator);
        }
    }
}
