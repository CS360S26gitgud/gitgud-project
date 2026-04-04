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
 * Handles new student account registration.
 * Connects to AuthController for Firebase Auth and Firestore writes.
 */
public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword;
    private Button btnRegister;
    private TextView tvGoToLogin;
    private AuthController authController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authController = new AuthController();

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        btnRegister.setOnClickListener(v -> handleRegister());
        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void handleRegister() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        authController.registerStudent(name, email, password, new AuthCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(RegisterActivity.this,
                        "Account created successfully!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(RegisterActivity.this,
                        "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}