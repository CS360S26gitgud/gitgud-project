package com.example.counsellingapp.view;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.counsellingapp.R;
import com.example.counsellingapp.controller.AuthCallback;
import com.example.counsellingapp.controller.AuthController;
import com.example.counsellingapp.model.Constants;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Handles new account self-registration for both students and counselors.
 *
 * <p>The form presents fields in the following order:
 * <ol>
 *   <li>Full Name</li>
 *   <li>Email</li>
 *   <li>Specialization (counselor-only dropdown, hidden by default — see below)</li>
 *   <li>Password</li>
 *   <li>Role selection (Student / Counselor radio buttons)</li>
 * </ol>
 *
 * <p>The Specialization field is a Material ExposedDropdownMenu positioned above
 * Password, populated from {@link Constants#SPECIALIZATIONS}. It is hidden
 * ({@code GONE}) until the user selects the Counselor radio button. When Student
 * is re-selected the field is hidden again and reset to the first item
 * ("General Practice") so stale selections are never submitted. Selecting
 * "General Practice" stores {@code null} in Firestore, consistent with the
 * {@link com.example.counsellingapp.model.Counselor} model convention that
 * {@code null} specialization = general practice.
 *
 * <p>On submit the activity delegates to the appropriate factory method on
 * {@link AuthController}:
 * <ul>
 *   <li>Student  → {@link AuthController#registerStudent} → writes a {@code Student}
 *       document to the {@code students/} collection.
 *   <li>Counselor → {@link AuthController#registerCounselor} → writes a {@code Counselor}
 *       document (unapproved) to the {@code counselors/} collection.
 * </ul>
 *
 * <p>CRC Responsibilities:
 * <ul>
 *   <li>Collect name, email, specialization (counselor only), password, and role.
 *   <li>Validate inputs and delegate object creation to {@link AuthController}.
 *   <li>Route to the correct dashboard via {@link LandingActivity#routeByRole(String, Context, Runnable)} on success.
 * </ul>
 *
 * CRC Collaborators: {@link AuthController}, {@link LandingActivity}, {@link LoginActivity}
 */
public class RegisterActivity extends AppCompatActivity {

    private EditText             etName, etEmail, etPassword;
    private AutoCompleteTextView actvSpecialization;
    private TextInputLayout      tilSpecialization;
    private RadioGroup           rgRole;
    private RadioButton          rbStudent, rbCounselor;
    private Button               btnRegister;
    private TextView             tvGoToLogin;

    private AuthController authController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authController = new AuthController();

        etName             = findViewById(R.id.etName);
        etEmail            = findViewById(R.id.etEmail);
        etPassword         = findViewById(R.id.etPassword);
        actvSpecialization = findViewById(R.id.actvSpecialization);
        tilSpecialization  = findViewById(R.id.tilSpecialization);
        rgRole             = findViewById(R.id.rgRole);
        rbStudent          = findViewById(R.id.rbStudent);
        rbCounselor        = findViewById(R.id.rbCounselor);
        btnRegister        = findViewById(R.id.btnRegister);
        tvGoToLogin        = findViewById(R.id.tvGoToLogin);

        // Populate the specialization dropdown from Constants.
        ArrayAdapter<String> specAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                Constants.SPECIALIZATIONS);
        actvSpecialization.setAdapter(specAdapter);
        // Pre-select the first item ("General Practice") as the default.
        actvSpecialization.setText(Constants.SPECIALIZATIONS.get(0), false);

        // Default role — student is pre-checked in XML; specialization stays hidden.
        tilSpecialization.setVisibility(View.GONE);

        // Show / hide the specialization dropdown depending on the selected role.
        rgRole.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbCounselor) {
                tilSpecialization.setVisibility(View.VISIBLE);
            } else {
                tilSpecialization.setVisibility(View.GONE);
                // Reset to default so a stale selection is never submitted.
                actvSpecialization.setText(Constants.SPECIALIZATIONS.get(0), false);
            }
        });

        btnRegister.setOnClickListener(v -> handleRegister());

        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    /**
     * Validates form fields and branches on the selected role to call the correct
     * {@link AuthController} factory method, producing either a {@code Student} or
     * {@code Counselor} object in Firestore.
     *
     * <p>Validation rules (shared):
     * <ul>
     *   <li>Name, email, and password must all be non-empty.
     *   <li>Password must be at least 6 characters (Firebase Auth minimum).
     *   <li>A role must be explicitly selected (one of Student / Counselor).
     * </ul>
     *
     * <p>Counselor-specific: the Specialization dropdown (displayed above the Password
     * field) always has a selection. Selecting "General Practice" maps to {@code null}
     * in Firestore, consistent with the {@link com.example.counsellingapp.model.Counselor}
     * convention that a {@code null} specialization means general practice.
     */
    private void handleRegister() {
        String name     = etName.getText().toString().trim();
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // --- Shared validation ---
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (rgRole.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select a role (Student or Counselor)",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegister.setEnabled(false);

        // --- Role branch: produce Student or Counselor object ---
        if (rbCounselor.isChecked()) {
            // Map "General Practice" → null to match the Counselor model convention.
            String selected = actvSpecialization.getText().toString().trim();
            String specialization = Constants.SPECIALIZATIONS.get(0).equals(selected)
                    ? null : selected;

            authController.registerCounselor(name, email, password, specialization,
                    new AuthCallback() {
                        @Override
                        public void onSuccess() {
                            routeAfterRegister();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            btnRegister.setEnabled(true);
                            Toast.makeText(RegisterActivity.this,
                                    "Registration failed: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        } else {
            // Student path (rbStudent.isChecked())
            authController.registerStudent(name, email, password, new AuthCallback() {
                @Override
                public void onSuccess() {
                    routeAfterRegister();
                }

                @Override
                public void onFailure(Exception e) {
                    btnRegister.setEnabled(true);
                    Toast.makeText(RegisterActivity.this,
                            "Registration failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    /**
     * Common post-registration routing. Delegates to {@link LandingActivity#routeByRole(String, Context, Runnable)}
     * so the correct dashboard is opened regardless of the registered role.
     */
    private void routeAfterRegister() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        LandingActivity.routeByRole(uid, RegisterActivity.this, () -> {
            btnRegister.setEnabled(true);
            Toast.makeText(RegisterActivity.this,
                    "Account created but could not load dashboard. Try logging in.",
                    Toast.LENGTH_LONG).show();
        });
    }
}