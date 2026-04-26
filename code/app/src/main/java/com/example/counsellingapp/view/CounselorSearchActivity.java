package com.example.counsellingapp.view;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.counsellingapp.R;
import com.example.counsellingapp.controller.CounselorController;
import com.example.counsellingapp.model.User;

import java.util.List;

/**
 * US-10 (existing): Search and filter counselors by specialization / available day.
 * US-07 (new hook): Each counselor card now has a "View Reviews" button via CounselorAdapter.
 *
 * Upgraded from ListView + ArrayAdapter<String> to RecyclerView + CounselorAdapter.
 * Controller logic is unchanged — only the presentation layer is updated.
 */
public class CounselorSearchActivity extends AppCompatActivity {

    private EditText     etSpecialization, etDay;
    private Button       btnSearch;
    private RecyclerView rvCounselors;

    private CounselorController counselorController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counselor_search);

        etSpecialization = findViewById(R.id.etSpecialization);
        etDay            = findViewById(R.id.etDay);
        btnSearch        = findViewById(R.id.btnSearch);
        rvCounselors     = findViewById(R.id.rvCounselors);

        counselorController = new CounselorController();
        rvCounselors.setLayoutManager(new LinearLayoutManager(this));

        btnSearch.setOnClickListener(v -> performSearch());
        performSearch(); // Populate on open
    }

    private void performSearch() {
        String spec = etSpecialization.getText().toString().trim();
        String day  = etDay.getText().toString().trim();

        counselorController.searchCounselors(spec, day, new CounselorController.CounselorListCallback() {
            @Override
            public void onSuccess(List<User> counselors) {
                rvCounselors.setAdapter(new CounselorAdapter(counselors));
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(CounselorSearchActivity.this,
                        "Search failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}