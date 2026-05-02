package com.example.counsellingapp.controller;

import com.example.counsellingapp.model.Counselor;
import com.example.counsellingapp.model.Student;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Handles Firebase Authentication and Firestore writes for login and registration.
 *
 * <p><b>Registration change (US-19):</b> The public registration path now only creates
 * {@link Student} accounts. Counselor accounts are created exclusively by an admin
 * through {@link AdminController#registerCounselor}. The {@link #registerUser} method
 * always writes to the {@code students/} collection regardless of any role parameter
 * that might be passed — counselor registration via this path is no longer supported.
 *
 * <p><b>Login change (US-18):</b> After Firebase Auth succeeds, {@link #loginUser}
 * fetches the user's document from the appropriate collection and checks type-specific
 * flags before allowing the session to proceed:
 * <ul>
 *   <li>Students: checked against {@link Student#isActive()}. An inactive student is
 *       signed out immediately and {@link AuthCallback#onFailure} is called with a
 *       descriptive message.
 *   <li>Counselors: the active check is handled at the routing level in
 *       {@code LandingActivity} (approval and suspension state control visibility,
 *       not login access, for counselors).
 *   <li>Admins: no additional checks; presence in the {@code admins/} collection is
 *       sufficient.
 * </ul>
 *
 * <p>CRC Responsibilities:
 * <ul>
 *   <li>Create student Firebase Auth accounts and write {@link Student} documents.
 *   <li>Authenticate users and enforce the student active flag on login.
 *   <li>Delegate role resolution and routing to {@code LandingActivity}.
 * </ul>
 *
 * CRC Collaborators: {@link Student}, {@link AuthCallback}, {@code LandingActivity}
 */
public class AuthController {

    private static final String COLLECTION_STUDENTS   = "students";
    private static final String COLLECTION_COUNSELORS = "counselors";

    private final FirebaseAuth      mAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db    = FirebaseFirestore.getInstance();

    /**
     * Creates a new Firebase Auth account and writes an active {@link Student} document
     * to the {@code students/} Firestore collection.
     *
     * <p>Counselor registration is not available through this method. Admins must use
     * {@link AdminController#registerCounselor} to create counselor accounts.
     *
     * @param name     The student's full display name.
     * @param email    Email address for the new account.
     * @param password Plain-text password (Firebase Auth handles hashing).
     * @param cb       Callback fired on write completion or failure.
     */
    public void registerUser(String name, String email, String password, AuthCallback cb) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String  uid     = result.getUser().getUid();
                    Student student = new Student(uid, name, email);
                    db.collection(COLLECTION_STUDENTS).document(uid)
                            .set(student)
                            .addOnSuccessListener(v -> cb.onSuccess())
                            .addOnFailureListener(cb::onFailure);
                })
                .addOnFailureListener(cb::onFailure);
    }

    /**
     * Authenticates an existing user via Firebase Auth, then checks type-specific
     * access flags before allowing the session to continue.
     *
     * <p>For students, the {@link Student#isActive()} flag is read from the
     * {@code students/} collection. If the account has been deactivated by an admin,
     * the Firebase Auth session is signed out immediately and
     * {@link AuthCallback#onFailure} is fired with an {@link IllegalStateException}
     * carrying a user-readable message.
     *
     * <p>For counselors and admins the method calls {@link AuthCallback#onSuccess}
     * immediately after Auth succeeds. Dashboard routing (which enforces counselor
     * approval and suspension visibility) is handled by {@code LandingActivity.routeByRole}.
     *
     * <p>If the UID is not found in the {@code students/} collection the method assumes
     * the user is a counselor or admin and proceeds to {@code onSuccess}, letting
     * the routing layer resolve the type.
     *
     * @param email    The user's registered email address.
     * @param password The user's password.
     * @param cb       Callback fired on authentication + active-check success or failure.
     */
    public void loginUser(String email, String password, AuthCallback cb) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    checkStudentActiveStatus(uid, cb);
                })
                .addOnFailureListener(cb::onFailure);
    }

    /**
     * Checks whether the UID belongs to an inactive student, signing them out if so.
     * If the UID is not in the {@code students/} collection, the check passes and
     * {@link AuthCallback#onSuccess} is called — the user is a counselor or admin.
     *
     * @param uid The Firebase UID of the freshly authenticated user.
     * @param cb  Callback from the parent {@link #loginUser} call.
     */
    private void checkStudentActiveStatus(String uid, AuthCallback cb) {
        db.collection(COLLECTION_STUDENTS).document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        // Not a student — counselor or admin, proceed normally
                        cb.onSuccess();
                        return;
                    }
                    Student student = doc.toObject(Student.class);
                    if (student != null && !student.isActive()) {
                        // Account deactivated by admin — sign out and reject
                        mAuth.signOut();
                        cb.onFailure(new IllegalStateException(
                                "Your account has been deactivated. "
                                        + "Please contact an administrator."));
                    } else {
                        cb.onSuccess();
                    }
                })
                .addOnFailureListener(cb::onFailure);
    }
}