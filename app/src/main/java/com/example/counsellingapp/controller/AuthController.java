package com.example.counsellingapp.controller;

import com.example.counsellingapp.model.Counselor;
import com.example.counsellingapp.model.Student;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Handles Firebase Authentication and Firestore writes for login and registration.
 *
 * <p><b>Registration:</b> Two distinct factory methods create the correct domain object:
 * <ul>
 *   <li>{@link #registerStudent}  — creates a {@link Student} in the {@code students/} collection.
 *   <li>{@link #registerCounselor} — creates a {@link Counselor} (unapproved) in the
 *       {@code counselors/} collection. Self-registered counselors must be approved by
 *       an admin via {@link AdminController#setCounselorApproved} before they appear to students.
 * </ul>
 *
 * <p><b>Login (US-18):</b> After Firebase Auth succeeds, {@link #loginUser}
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
 *   <li>Create Student or Counselor Firebase Auth accounts and write the correct
 *       typed Firestore document via the appropriate factory method.
 *   <li>Authenticate users and enforce the student active flag on login.
 *   <li>Delegate role resolution and routing to {@code LandingActivity}.
 * </ul>
 *
 * CRC Collaborators: {@link Student}, {@link Counselor}, {@link AuthCallback},
 *                    {@code LandingActivity}
 */
public class AuthController {

    private static final String COLLECTION_STUDENTS   = "students";
    private static final String COLLECTION_COUNSELORS = "counselors";

    private final FirebaseAuth      mAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db    = FirebaseFirestore.getInstance();

    // -------------------------------------------------------------------------
    // Registration — one method per concrete type
    // -------------------------------------------------------------------------

    /**
     * Creates a new Firebase Auth account and writes an active {@link Student} document
     * to the {@code students/} Firestore collection.
     *
     * @param name     The student's full display name.
     * @param email    Email address for the new account.
     * @param password Plain-text password (Firebase Auth handles hashing).
     * @param cb       Callback fired on write completion or failure.
     */
    public void registerStudent(String name, String email, String password, AuthCallback cb) {
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
     * Creates a new Firebase Auth account and writes an unapproved {@link Counselor}
     * document to the {@code counselors/} Firestore collection.
     *
     * <p>The counselor starts with {@code approved = false} and must be explicitly
     * approved by an admin via {@link AdminController#setCounselorApproved} before
     * appearing in any student-facing search.
     *
     * <p>If {@code specialization} is {@code null} or blank the field is stored as
     * {@code null} in Firestore; the UI convention is to display "General" in that case.
     *
     * @param name           The counselor's full display name.
     * @param email          Email address for the new account.
     * @param password       Plain-text password.
     * @param specialization Area of expertise, or {@code null} / empty for general practice.
     * @param cb             Callback fired on write completion or failure.
     */
    public void registerCounselor(String name, String email, String password,
                                  String specialization, AuthCallback cb) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid  = result.getUser().getUid();
                    // Normalise: blank string → null so Firestore stores null cleanly.
                    String spec = (specialization == null || specialization.trim().isEmpty())
                            ? null : specialization.trim();
                    Counselor counselor = new Counselor(uid, name, email, spec, null);
                    db.collection(COLLECTION_COUNSELORS).document(uid)
                            .set(counselor)
                            .addOnSuccessListener(v -> cb.onSuccess())
                            .addOnFailureListener(cb::onFailure);
                })
                .addOnFailureListener(cb::onFailure);
    }

    /**
     * Kept for backward compatibility with any call-sites that still reference the old
     * single-method signature. Delegates directly to {@link #registerStudent}.
     *
     * @deprecated Use {@link #registerStudent} or {@link #registerCounselor} explicitly.
     */
    @Deprecated
    public void registerUser(String name, String email, String password, AuthCallback cb) {
        registerStudent(name, email, password, cb);
    }

    // -------------------------------------------------------------------------
    // Login
    // -------------------------------------------------------------------------

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
                        // Not a student — counselor or admin, proceed normally.
                        cb.onSuccess();
                        return;
                    }
                    Student student = doc.toObject(Student.class);
                    if (student != null && !student.isActive()) {
                        // Account deactivated by admin — sign out and reject.
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