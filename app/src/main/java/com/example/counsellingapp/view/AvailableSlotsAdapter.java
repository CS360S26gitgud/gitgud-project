package com.example.counsellingapp.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.counsellingapp.R;
import com.example.counsellingapp.model.TimeSlot;
import java.util.List;

public class AvailableSlotsAdapter extends RecyclerView.Adapter<AvailableSlotsAdapter.SlotViewHolder> {

    private List<TimeSlot> slots;
    private OnSlotBookClickListener clickListener;

    public interface OnSlotBookClickListener {
        void onBookClicked(TimeSlot slot);
    }

    public AvailableSlotsAdapter(List<TimeSlot> slots, OnSlotBookClickListener listener) {
        this.slots = slots;
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public SlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_slot, parent, false);
        return new SlotViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SlotViewHolder holder, int position) {
        TimeSlot slot = slots.get(position);

        // Check if we populated the name; if not fallback to ID
        String displayName = slot.getCounselorName() != null ? slot.getCounselorName() : "ID: " + slot.getCounselorId();
        holder.tvCounselorName.setText(displayName);
        
        String spec = slot.getSpecialization() != null ? slot.getSpecialization() : "General Practice";
        holder.tvSpecialization.setText(spec);

        holder.tvTime.setText("Date: " + slot.getDate() + " | " + slot.getStartTime() + " - " + slot.getEndTime());

        holder.btnBookSlot.setOnClickListener(v -> clickListener.onBookClicked(slot));
    }

    @Override
    public int getItemCount() {
        return slots == null ? 0 : slots.size();
    }

    public void updateData(List<TimeSlot> newSlots) {
        this.slots = newSlots;
        notifyDataSetChanged();
    }

    public static class SlotViewHolder extends RecyclerView.ViewHolder {
        TextView tvCounselorName, tvSpecialization, tvTime;
        Button btnBookSlot;

        public SlotViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCounselorName = itemView.findViewById(R.id.tvCounselorName);
            tvSpecialization = itemView.findViewById(R.id.tvSpecialization);
            tvTime = itemView.findViewById(R.id.tvTime);
            btnBookSlot = itemView.findViewById(R.id.btnBookSlot);
        }
    }

}
