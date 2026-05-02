package com.example.counsellingapp.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.counsellingapp.R;
import com.example.counsellingapp.controller.AdminController;
import com.example.counsellingapp.controller.AuthCallback;
import com.example.counsellingapp.controller.AuthController;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Handles new <em>student</em> account self-registration.
 *
 * <p><b>US-19 change:</b> The counselor registration option (radio button) has been
 * removed. Counselor accounts can only be created by an admin through
 * {@link AdminDashboardActivity} → {@link AdminController#registerCounselor}. This
 * ensures that only accounts explicitly authorized by an admin can ever become counselors,
 * satisfying the requirement that only authorized counselors appear to students.
 *
 * <p>The role radio group and {@code rbCounselor} view are no longer needed and have
 * been removed from both this Activity and its layout. {@link AuthController#registerUser}
 * always creates a {@code Student} document, so no role parameter is passed.
 *
 * <p>CRC Responsibilities:
 * <ul>
 *   <li>Collect name, email, and password from the student.
 *   <li>Validate inputs and delegate account creation to {@link AuthController}.
 *   <li>Route to the student dashboard via {@link LandingActivity#routeByRole} on success.
 * </ul>
 *
 * CRC Collaborators: {@link AuthController}, {@link LandingActivity}, {@link LoginActivity}
 */
public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword;
    private Button   btnRegister;
    private TextView tvGoToLogin;

    private AuthController authController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authController = new AuthController();

        etName      = findViewById(R.id.etName);
        etEmail     = findViewById(R.id.etEmail);
        etPassword  = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        btnRegister.setOnClickListener(v -> handleRegister());

        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    /**
     * Validates the form fields and delegates student account creation to
     * {@link AuthController#registerUser}.
     *
     * <p>Validation rules:
     * <ul>
     *   <li>Name, email, and password must all be non-empty.
     *   <li>Password must be at least 6 characters (Firebase Auth minimum).
     * </ul>
     *
     * <p>On success, routes directly to the student dashboard via
     * {@link LandingActivity#routeByRole} — the same path taken after login.
     */
    private void handleRegister() {
        String name     = etName.getText().toString().trim();
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegister.setEnabled(false);

        authController.registerUser(name, email, password, new AuthCallback() {
            @Override
            public void onSuccess() {
                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
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