package com.example.counsellingapp.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.counsellingapp.R;
import com.example.counsellingapp.controller.AuthCallback;
import com.example.counsellingapp.controller.AuthController;

/**
 * Handles login for students and counselors.
 * Role is checked against Firestore after Firebase Auth succeeds.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvGoToRegister;
    private AuthController authController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authController = new AuthController();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);

        btnLogin.setOnClickListener(v -> handleLogin());
        tvGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });
    }

    private void handleLogin() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);

        // No role passed — AuthController just authenticates, routing handles the rest
        authController.loginUser(email, password, new AuthCallback() {
            @Override
            public void onSuccess() {
                String uid = com.google.firebase.auth.FirebaseAuth
                        .getInstance().getCurrentUser().getUid();
                LandingActivity.routeByRole(uid, LoginActivity.this, () -> {
                    btnLogin.setEnabled(true);
                    Toast.makeText(LoginActivity.this,
                            "Could not load account details. Try again.",
                            Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onFailure(Exception e) {
                btnLogin.setEnabled(true);
                Toast.makeText(LoginActivity.this,
                        "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}