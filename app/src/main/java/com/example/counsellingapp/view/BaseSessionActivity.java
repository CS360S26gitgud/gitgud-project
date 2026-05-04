package com.example.counsellingapp.view;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

/**
 * Base activity that enforces an inactivity session timeout for student-facing screens.
 *
 * <p>All student-facing activities extend this class instead of {@link AppCompatActivity}
 * directly. The timeout satisfies US-02 AC4: a session must expire after a period of
 * inactivity so that personal data is not exposed on an unattended device.
 *
 * <p><b>Timeout behaviour:</b>
 * <ul>
 *   <li>The inactivity countdown starts (or resets) every time the user touches the
 *       screen, presses a key, or otherwise interacts with the UI — via
 *       {@link #onUserInteraction()}.
 *   <li>The timer is also reset on {@link #onResume()} so that switching between
 *       student screens does not silently drain the countdown.
 *   <li>The timer is cancelled on {@link #onPause()} so that background time (e.g.
 *       the device screen being off) does not count toward the timeout.
 *   <li>When the timer fires the user is signed out of Firebase Auth, a Toast is shown,
 *       and {@link LoginActivity} is launched with
 *       {@code FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK} so the entire back
 *       stack is cleared and the student cannot press Back to return to a dashboard.
 * </ul>
 *
 * <p><b>Timeout duration:</b> {@link #TIMEOUT_MS} — 10 minutes by default.
 *
 * <p>CRC Responsibilities:
 * <ul>
 *   <li>Manage the inactivity timer lifecycle across activity start, resume, pause, and
 *       user interaction events.
 *   <li>Sign out and redirect to {@link LoginActivity} when the timer fires.
 * </ul>
 *
 * CRC Collaborators: {@link LoginActivity}, {@link FirebaseAuth}
 */
public abstract class BaseSessionActivity extends AppCompatActivity {

    /**
     * Inactivity period in milliseconds before the session is automatically terminated.
     * Default is 10 minutes. Adjust here to change the timeout app-wide.
     */
    private static final long TIMEOUT_MS = 10 * 60 * 1000L;

    private final Handler  sessionHandler = new Handler(Looper.getMainLooper());
    private final Runnable sessionTimeout = this::onSessionTimeout;

    // -------------------------------------------------------------------------
    // Lifecycle hooks
    // -------------------------------------------------------------------------

    /**
     * Resets the inactivity timer whenever the activity becomes visible.
     * Subclasses that override {@code onResume} must call {@code super.onResume()}.
     */
    @Override
    protected void onResume() {
        super.onResume();
        resetTimer();
    }

    /**
     * Cancels the inactivity timer when the activity is no longer in the foreground.
     * This prevents background time from counting toward the timeout.
     * Subclasses that override {@code onPause} must call {@code super.onPause()}.
     */
    @Override
    protected void onPause() {
        super.onPause();
        cancelTimer();
    }

    /**
     * Resets the inactivity timer on every user interaction (touch, key press, etc.).
     * Called automatically by the Android framework before the event is dispatched.
     */
    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        resetTimer();
    }

    // -------------------------------------------------------------------------
    // Timer helpers
    // -------------------------------------------------------------------------

    /**
     * Cancels any pending timeout and posts a fresh one for {@link #TIMEOUT_MS} ms.
     */
    private void resetTimer() {
        sessionHandler.removeCallbacks(sessionTimeout);
        sessionHandler.postDelayed(sessionTimeout, TIMEOUT_MS);
    }

    /**
     * Cancels any pending timeout without scheduling a new one.
     */
    private void cancelTimer() {
        sessionHandler.removeCallbacks(sessionTimeout);
    }

    // -------------------------------------------------------------------------
    // Timeout action
    // -------------------------------------------------------------------------

    /**
     * Called when the inactivity timer fires. Signs the user out of Firebase Auth,
     * shows a timeout notice, and navigates to {@link LoginActivity} with a cleared
     * back stack so the student cannot return to the dashboard via the Back button.
     */
    private void onSessionTimeout() {
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(this,
                "You have been signed out due to inactivity.",
                Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
