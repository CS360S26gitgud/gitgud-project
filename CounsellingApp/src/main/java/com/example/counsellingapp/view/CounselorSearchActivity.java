package com.example.counsellingapp.view;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.counsellingapp.R;
import com.example.counsellingapp.controller.CounselorController;
import com.example.counsellingapp.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for searching and filtering counselors (US 10).
 */
public class CounselorSearchActivity extends AppCompatActivity {

    private EditText etSpecialization, etDay;
    private Button btnSearch;
    private ListView lvCounselors;
    private CounselorController counselorController;
    private List<String> counselorDisplayList;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counselor_search);

        etSpecialization = findViewById(R.id.etSpecialization);
        etDay = findViewById(R.id.etDay);
        btnSearch = findViewById(R.id.btnSearch);
        lvCounselors = findViewById(R.id.lvCounselors);

        counselorController = new CounselorController();
        counselorDisplayList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, counselorDisplayList);
        lvCounselors.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> performSearch());

        // Load all initially
        performSearch();
    }

    private void performSearch() {
        String spec = etSpecialization.getText().toString().trim();
        String day = etDay.getText().toString().trim();

        counselorController.searchCounselors(spec, day, new CounselorController.CounselorListCallback() {
            @Override
            public void onSuccess(List<User> counselors) {
                counselorDisplayList.clear();
                for (User c : counselors) {
                    counselorDisplayList.add(c.getName() + " - " + (c.getSpecialization() != null ? c.getSpecialization() : "General"));
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(CounselorSearchActivity.this, "Search failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}