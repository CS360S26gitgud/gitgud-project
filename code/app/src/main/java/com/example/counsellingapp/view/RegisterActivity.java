package com.example.counsellingapp.view;

import android.content.Intent;
import android.os.Bundle;
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
import com.google.firebase.auth.FirebaseAuth;

/**
 * Handles new student account registration.
 * Connects to AuthController for Firebase Auth and Firestore writes.
 */
public class RegisterActivity extends AppCompatActivity {

    private EditText    etName, etEmail, etPassword;
    private RadioGroup rgRole;
    private RadioButton rbStudent, rbCounselor;
    private Button      btnRegister;
    private TextView    tvGoToLogin;

    private AuthController authController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authController = new AuthController();

        etName      = findViewById(R.id.etName);
        etEmail     = findViewById(R.id.etEmail);
        etPassword  = findViewById(R.id.etPassword);
        rgRole      = findViewById(R.id.rgRole);
        rbStudent   = findViewById(R.id.rbStudent);
        rbCounselor = findViewById(R.id.rbCounselor);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        // Default selection — student is the common case
        rbStudent.setChecked(true);

        btnRegister.setOnClickListener(v -> handleRegister());

        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void handleRegister() {
        String name     = etName.getText().toString().trim();
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (rgRole.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
            return;
        }

        // Derive role string from whichever radio button is checked
        String role = (rgRole.getCheckedRadioButtonId() == R.id.rbCounselor) ? "counselor" : "student";

        btnRegister.setEnabled(false);

        authController.registerUser(name, email, password, role, new AuthCallback() {
            @Override
            public void onSuccess() {
                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                // Reuse the same routing logic as login — role is now in Firestore
                LandingActivity.routeByRole(uid, RegisterActivity.this, () -> {
                    btnRegister.setEnabled(true);
                    Toast.makeText(RegisterActivity.this,
                            "Account created but could not load dashboard. Try logging in.",
                            Toast.LENGTH_LONG).show();
                });
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