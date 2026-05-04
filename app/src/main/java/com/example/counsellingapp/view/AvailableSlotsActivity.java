package com.example.counsellingapp.view;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.counsellingapp.R;
import com.example.counsellingapp.controller.AppointmentController;
import com.example.counsellingapp.controller.AvailabilityController;
import com.example.counsellingapp.model.TimeSlot;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class AvailableSlotsActivity extends BaseSessionActivity {

    private RecyclerView recyclerView;
    private AvailableSlotsAdapter adapter;
    private android.widget.Spinner spSpecialization, spDay;

    private AvailabilityController availabilityController;
    private AppointmentController appointmentController;
    private List<TimeSlot> allSlots = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_available_slots);

        recyclerView = findViewById(R.id.recyclerViewSlots);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AvailableSlotsAdapter(new ArrayList<>(), this::confirmBooking);
        recyclerView.setAdapter(adapter);

        spSpecialization = findViewById(R.id.spSpecialization);
        spDay = findViewById(R.id.spDay);

        setupFilters();

        availabilityController = new AvailabilityController();
        appointmentController = new AppointmentController();

        fetchSlots();
    }

    private void setupFilters() {
        List<String> specs = new ArrayList<>();
        specs.add("All Specializations");
        specs.addAll(com.example.counsellingapp.model.Constants.SPECIALIZATIONS);

        android.widget.ArrayAdapter<String> specAdapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, specs);
        spSpecialization.setAdapter(specAdapter);

        List<String> days = new ArrayList<>();
        days.add("All Days");
        days.addAll(com.example.counsellingapp.model.Constants.DAYS);

        android.widget.ArrayAdapter<String> dayAdapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, days);
        spDay.setAdapter(dayAdapter);

        android.widget.AdapterView.OnItemSelectedListener listener = new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, android.view.View v, int i, long l) { applyFilters(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        };

        spSpecialization.setOnItemSelectedListener(listener);
        spDay.setOnItemSelectedListener(listener);
    }

    private void applyFilters() {
        String selectedSpec = spSpecialization.getSelectedItem().toString();
        String selectedDay = spDay.getSelectedItem().toString();

        List<TimeSlot> filtered = new ArrayList<>();
        for (TimeSlot slot : allSlots) {
            boolean matchesSpec = selectedSpec.equals("All Specializations") ||
                    (slot.getSpecialization() != null && slot.getSpecialization().equals(selectedSpec));

            // Derive day from date string YYYY-MM-DD
            String slotDay = getDayFromDate(slot.getDate());
            boolean matchesDay = selectedDay.equals("All Days") ||
                    (slotDay != null && slotDay.equalsIgnoreCase(selectedDay));

            if (matchesSpec && matchesDay) {
                filtered.add(slot);
            }
        }
        adapter.updateData(filtered);
    }

    private String getDayFromDate(String dateStr) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            java.util.Date date = sdf.parse(dateStr);
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(date);
            return cal.getDisplayName(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.LONG, java.util.Locale.US);
        } catch (Exception e) { return null; }
    }

    private void fetchSlots() {
        availabilityController.getAllAvailableSlots(new AvailabilityController.SlotListCallback() {
            @Override
            public void onSuccess(List<TimeSlot> slots) {
                allSlots = slots;
                applyFilters();
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