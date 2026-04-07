package com.example.counsellingapp.view;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.counsellingapp.R;
import com.example.counsellingapp.controller.AppointmentController;
import com.example.counsellingapp.model.Appointment;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Activity for viewing appointment history (US 11).
 */
public class AppointmentHistoryActivity extends AppCompatActivity {

    private ListView lvAppointments;
    private AppointmentController appointmentController;
    private List<String> appointmentDisplayList;
    private ArrayAdapter<String> adapter;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_history);

        lvAppointments = findViewById(R.id.lvAppointments);
        appointmentController = new AppointmentController();
        appointmentDisplayList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, appointmentDisplayList);
        lvAppointments.setAdapter(adapter);

        loadHistory();
    }

    private void loadHistory() {
        String studentId = FirebaseAuth.getInstance().getUid();
        if (studentId == null) return;

        appointmentController.getStudentAppointmentHistory(studentId, new AppointmentController.AppointmentListCallback() {
            @Override
            public void onSuccess(List<Appointment> appointments) {
                appointmentDisplayList.clear();
                for (Appointment appt : appointments) {
                    String dateStr = appt.getDateTime() != null ? dateFormat.format(appt.getDateTime().toDate()) : "N/A";
                    appointmentDisplayList.add("With: " + appt.getCounselorName() + "\nDate: " + dateStr + "\nStatus: " + appt.getStatus());
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(AppointmentHistoryActivity.this, "Failed to load history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}