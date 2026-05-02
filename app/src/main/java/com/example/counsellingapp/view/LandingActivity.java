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
 * Entry point of the app. Handles session recovery and initial role-based routing.
 *
 * <p>If a Firebase Auth session already exists when the activity starts, the user's
 * role is determined by checking which Firestore collection contains their UID, then
 * the correct dashboard is opened without showing the login/register screen.
 *
 * <p><b>Routing strategy:</b> Collections are checked in priority order:
 * <ol>
 *   <li>{@code admins/{uid}} — routes to {@link AdminDashboardActivity}.
 *   <li>{@code counselors/{uid}} — routes to {@link CounselorDashboardActivity}.
 *   <li>Neither found — routes to {@link StudentDashboardActivity}.
 * </ol>
 *
 * <p>This replaces the former string-comparison approach ({@code role == "counselor"})
 * with a collection-existence check. The collection itself encodes the type; there is
 * no {@code role} field on any user document. Checking admins first ensures an admin
 * UID cannot accidentally fall through to a student or counselor dashboard, fixing
 * the prior bug where admin accounts silently landed on the student dashboard.
 *
 * <p>{@link #routeByRole(String, android.content.Context, Runnable)} is public and static
 * so {@link LoginActivity} and {@link RegisterActivity} can reuse the same routing logic
 * after a successful authentication without duplicating it.
 *
 * <p>CRC Responsibilities:
 * <ul>
 *   <li>Check for an existing Firebase Auth session on launch.
 *   <li>Route authenticated users to the correct dashboard.
 *   <li>Present login and register entry points for unauthenticated users.
 * </ul>
 *
 * CRC Collaborators: {@link LoginActivity}, {@link RegisterActivity},
 *                    {@link StudentDashboardActivity}, {@link CounselorDashboardActivity},
 *                    {@link AdminDashboardActivity}
 */
public class LandingActivity extends AppCompatActivity {

    private static final String COLLECTION_ADMINS     = "admins";
    private static final String COLLECTION_COUNSELORS = "counselors";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            routeByRole(currentUser.getUid());
            return;
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
     * Determines the user's role via collection-existence checks and navigates to the
     * correct dashboard. Checks are performed in priority order: admin → counselor →
     * student (default).
     *
     * <p>Step 1: Check {@code admins/{uid}}.
     * <br>Step 2: If not admin, check {@code counselors/{uid}}.
     * <br>Step 3: If neither, open the student dashboard (covers all student accounts).
     *
     * <p>This method is {@code public static} so {@link LoginActivity} and
     * {@link RegisterActivity} can call it after successful authentication.
     *
     * @param uid       Firebase Auth UID of the user to route.
     * @param context   Android context used to build and fire the dashboard {@link Intent}.
     * @param onFailure {@link Runnable} invoked if any Firestore read fails; the caller
     *                  should show an error and optionally sign the user out.
     */
    public static void routeByRole(String uid, android.content.Context context,
                                   Runnable onFailure) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Step 1: check admins collection
        db.collection(COLLECTION_ADMINS).document(uid)
                .get()
                .addOnSuccessListener(adminDoc -> {
                    if (adminDoc.exists()) {
                        launchDashboard(context, AdminDashboardActivity.class);
                        return;
                    }
                    // Step 2: check counselors collection
                    db.collection(COLLECTION_COUNSELORS).document(uid)
                            .get()
                            .addOnSuccessListener(counselorDoc -> {
                                if (counselorDoc.exists()) {
                                    launchDashboard(context, CounselorDashboardActivity.class);
                                } else {
                                    // Step 3: default to student
                                    launchDashboard(context, StudentDashboardActivity.class);
                                }
                            })
                            .addOnFailureListener(e -> onFailure.run());
                })
                .addOnFailureListener(e -> onFailure.run());
    }

    /**
     * Starts a dashboard activity, clearing the back stack so the user cannot
     * navigate back to the login/landing screen.
     *
     * @param context       Android context for the {@link Intent}.
     * @param dashboardClass The dashboard activity class to launch.
     */
    private static void launchDashboard(android.content.Context context,
                                        Class<?> dashboardClass) {
        Intent intent = new Intent(context, dashboardClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    /**
     * Convenience overload for use inside {@link LandingActivity} itself.
     * On failure, signs the user out and falls back to showing the landing screen.
     *
     * @param uid Firebase Auth UID of the user to route.
     */
    private void routeByRole(String uid) {
        routeByRole(uid, this, () -> {
            FirebaseAuth.getInstance().signOut();
            setContentView(R.layout.activity_landing);
        });
    }
}