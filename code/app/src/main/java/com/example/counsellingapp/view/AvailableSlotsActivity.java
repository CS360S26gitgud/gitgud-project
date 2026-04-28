package com.example.counsellingapp.view;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.counsellingapp.R;
import com.example.counsellingapp.controller.AppointmentController;
import com.example.counsellingapp.controller.AvailabilityController;
import com.example.counsellingapp.model.TimeSlot;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class AvailableSlotsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AvailableSlotsAdapter adapter;

    private AvailabilityController availabilityController;
    private AppointmentController appointmentController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_available_slots);

        recyclerView = findViewById(R.id.recyclerViewSlots);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AvailableSlotsAdapter(new ArrayList<>(), this::confirmBooking);
        recyclerView.setAdapter(adapter);

        availabilityController = new AvailabilityController();
        appointmentController = new AppointmentController();

        fetchSlots();
    }

    private void fetchSlots() {
        availabilityController.getAllAvailableSlots(new AvailabilityController.SlotListCallback() {
            @Override
            public void onSuccess(List<TimeSlot> slots) {
                adapter.updateData(slots);
                if(slots.isEmpty()) {
                    Toast.makeText(AvailableSlotsActivity.this, "No slots available.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(AvailableSlotsActivity.this, "Failed to load slots.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmBooking(TimeSlot slot) {
        new AlertDialog.Builder(this)
                .setTitle("Book Appointment")
                .setMessage("Book slot on " + slot.getDate() + " at " + slot.getStartTime() + "?")
                .setPositiveButton("Yes", (dialog, which) -> bookSlot(slot))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void bookSlot(TimeSlot slot) {
        String studentId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // US-08: Pass context to enable local notifications from the controller
        appointmentController.bookSlot(this, slot, studentId, new AppointmentController.BookingCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(AvailableSlotsActivity.this, "Booked Successfully!", Toast.LENGTH_LONG).show();
                fetchSlots(); // Refresh screen
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(AvailableSlotsActivity.this, "Booking failed.", Toast.LENGTH_LONG).show();
                fetchSlots();
            }
        });
    }
}
