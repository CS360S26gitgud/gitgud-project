package com.example.counsellingapp.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.counsellingapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Entry point of the app.
 * If a session already exists, resolves the user's role from Firestore
 * and routes directly to the correct dashboard — no login screen shown.
 * Otherwise presents Login / Register options.
 */
public class LandingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check for an existing session before showing any UI
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            routeByRole(currentUser.getUid());
            return; // don't inflate the layout while we're routing
        }

        setContentView(R.layout.activity_landing);

        Button btnLogin    = findViewById(R.id.btnGoToLogin);
        Button btnRegister = findViewById(R.id.btnGoToRegister);

        btnLogin.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));

        btnRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    /**
     * Looks up the user's role in Firestore and navigates to the
     * correct dashboard. Called both on auto-login and after manual login.
     * Public and static so LoginActivity can reuse it after a successful login.
     */
    public static void routeByRole(String uid,
                                   android.content.Context context,
                                   Runnable onFailure) {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    String role = doc.getString("role");
                    Intent intent;
                    if ("counselor".equals(role)) {
                        intent = new Intent(context, CounselorDashboardActivity.class);
                    } else {
                        // Defaults to student dashboard for role == "student"
                        intent = new Intent(context, StudentDashboardActivity.class);
                    }
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    context.startActivity(intent);
                })
                .addOnFailureListener(e -> onFailure.run());
    }

    // Convenience overload for use inside LandingActivity itself
    private void routeByRole(String uid) {
        routeByRole(uid, this, () -> {
            // If Firestore fails, sign out and show landing screen normally
            FirebaseAuth.getInstance().signOut();
            setContentView(R.layout.activity_landing);
        });
    }
}