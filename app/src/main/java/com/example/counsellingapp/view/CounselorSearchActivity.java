package com.example.counsellingapp.view;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.counsellingapp.R;
import com.example.counsellingapp.controller.CounselorController;
import com.example.counsellingapp.model.Constants;
import com.example.counsellingapp.model.Counselor;

import java.util.ArrayList;
import java.util.List;


/**
 * US-10 (existing): Search and filter counselors by specialization / available day.
 * US-07 (new hook): Each counselor card now has a "View Reviews" button via CounselorAdapter.
 *
 * Upgraded from ListView + ArrayAdapter<String> to RecyclerView + CounselorAdapter.
 * Controller logic is unchanged — only the presentation layer is updated.
 *
 * Fix: callback type corrected from List<User> to List<Counselor> to match
 * CounselorController.CounselorListCallback signature.
 */
public class CounselorSearchActivity extends AppCompatActivity {

    private Spinner      spSpecialization, spDay;
    private Button       btnSearch;
    private RecyclerView rvCounselors;


    private CounselorController counselorController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counselor_search);

        spSpecialization = findViewById(R.id.spSearchSpec);
        spDay            = findViewById(R.id.spSearchDay);
        btnSearch        = findViewById(R.id.btnSearch);
        rvCounselors     = findViewById(R.id.rvCounselors);

        counselorController = new CounselorController();
        rvCounselors.setLayoutManager(new LinearLayoutManager(this));

        setupSpinners();

        btnSearch.setOnClickListener(v -> performSearch());
        performSearch(); // Populate on open
    }

    private void setupSpinners() {
        // Specialization Spinner
        List<String> specs = new ArrayList<>();
        specs.add("All Specializations");
        specs.addAll(Constants.SPECIALIZATIONS);
        ArrayAdapter<String> specAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, specs);
        specAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSpecialization.setAdapter(specAdapter);

        // Day Spinner
        List<String> days = new ArrayList<>();
        days.add("Any Day");
        days.addAll(Constants.DAYS);
        ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, days);
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDay.setAdapter(dayAdapter);
    }

    private void performSearch() {
        String spec = spSpecialization.getSelectedItem().toString();
        String day  = spDay.getSelectedItem().toString();

        if (spec.equals("All Specializations")) spec = "";
        if (day.equals("Any Day")) day = "";


        counselorController.searchCounselors(spec, day, new CounselorController.CounselorListCallback() {
            @Override
            public void onSuccess(List<Counselor> counselors) {   // was List<User> — now correct
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
