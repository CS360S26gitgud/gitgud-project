package com.example.counsellingapp.view;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.counsellingapp.R;
import com.example.counsellingapp.controller.AvailabilityCallback;
import com.example.counsellingapp.controller.AvailabilityController;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Calendar;
import java.util.Locale;

/**
 * US-13: Allows a counselor to set or update a single availability time slot.
 * Date and times are chosen via system dialogs to prevent invalid free-text input.
 * Outstanding issue: recurring-slot creation deferred to a later sprint.
 */
public class SetAvailabilityActivity extends AppCompatActivity {

    private Button      btnPickDate, btnPickStart, btnPickEnd, btnSave;
    private ProgressBar progressBar;

    private String selectedDate      = "";
    private String selectedStartTime = "";
    private String selectedEndTime   = "";

    private AvailabilityController availabilityController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_availability);

        availabilityController = new AvailabilityController();

        btnPickDate  = findViewById(R.id.btnPickDate);
        btnPickStart = findViewById(R.id.btnPickStartTime);
        btnPickEnd   = findViewById(R.id.btnPickEndTime);
        btnSave      = findViewById(R.id.btnSaveSlot);
        progressBar  = findViewById(R.id.progressBarAvailability);

        btnPickDate.setOnClickListener(v  -> showDatePicker());
        btnPickStart.setOnClickListener(v -> showTimePicker(true));
        btnPickEnd.setOnClickListener(v   -> showTimePicker(false));
        btnSave.setOnClickListener(v      -> saveSlot());
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedDate = String.format(Locale.getDefault(),
                    "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            btnPickDate.setText("Date: " + selectedDate);
        },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker(boolean isStart) {
        Calendar cal = Calendar.getInstance();
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
            if (isStart) {
                selectedStartTime = time;
                btnPickStart.setText("Start: " + time);
            } else {
                selectedEndTime = time;
                btnPickEnd.setText("End: " + time);
            }
        },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true).show();
    }

    private void saveSlot() {
        if (selectedDate.isEmpty() || selectedStartTime.isEmpty() || selectedEndTime.isEmpty()) {
            Toast.makeText(this, "Please select date, start time, and end time",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Guard: end time must be after start time
        if (selectedEndTime.compareTo(selectedStartTime) <= 0) {
            Toast.makeText(this, "End time must be after start time",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Session expired. Please log in again.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        availabilityController.addSlot(
                user.getUid(), selectedDate, selectedStartTime, selectedEndTime,
                new AvailabilityCallback() {
                    @Override
                    public void onSuccess() {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(SetAvailabilityActivity.this,
                                "Availability saved!", Toast.LENGTH_SHORT).show();
                        finish(); // return to dashboard; onResume() will refresh the list
                    }

                    @Override
                    public void onFailure(Exception e) {
                        progressBar.setVisibility(View.GONE);
                        btnSave.setEnabled(true);
                        Toast.makeText(SetAvailabilityActivity.this,
                                "Failed to save: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}