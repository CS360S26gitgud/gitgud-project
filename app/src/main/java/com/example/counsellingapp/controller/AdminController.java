package com.example.counsellingapp.controller;

import com.example.counsellingapp.model.Counselor;
import com.example.counsellingapp.model.Student;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * Controller for all admin-facing operations across US-18, US-19, and US-20.
 *
 * <p>All methods mutate or read documents in the {@code students/} or
 * {@code counselors/} Firestore collections and must only be called from
 * admin-authenticated screens ({@link AdminDashboardActivity}).
 *
 * <p><b>US-18 — Student account management:</b>
 * <ul>
 *   <li>{@link #createStudent} — admin creates a new student account (criterion 1).
 *   <li>{@link #updateStudent} — admin updates a student's name and/or email (criterion 3).
 *   <li>{@link #setStudentActive} — admin activates or deactivates a student account
 *       (criteria 2 and 4). Students cannot call this method themselves.
 * </ul>
 *
 * <p><b>US-19 — Counselor profile and permission management:</b>
 * <ul>
 *   <li>{@link #registerCounselor} — admin creates a new counselor account. This is
 *       the <em>only</em> path for counselor registration; the public
 *       {@link RegisterActivity} only allows student self-registration (criterion: only
 *       authorized counselors can accept appointments).
 *   <li>{@link #setCounselorApproved} — admin approves or revokes a counselor
 *       (criteria 1 and 2). Unapproved counselors are hidden from student queries.
 *   <li>{@link #updateCounselor} — admin updates a counselor's profile fields (criterion 2).
 * </ul>
 *
 * <p><b>US-20 — Suspension clearance after admin meeting:</b>
 * <ul>
 *   <li>{@link #clearCounselorSuspension} — admin marks the post-suspension meeting as
 *       complete, atomically lifting the suspension so the counselor reappears to students.
 *       This is the only operation that can lift a rating-triggered suspension.
 * </ul>
 *
 * CRC Collaborators: {@link Student}, {@link Counselor}, {@link AdminDashboardActivity}
 */
public class AdminController {

    private static final String COLLECTION_STUDENTS   = "students";
    private static final String COLLECTION_COUNSELORS = "counselors";

    private final FirebaseFirestore db    = FirebaseFirestore.getInstance();
    private final FirebaseAuth      mAuth = FirebaseAuth.getInstance();
    private final ActivityController activityController = new ActivityController();


    // -------------------------------------------------------------------------
    // Callback interfaces
    // -------------------------------------------------------------------------

    /**
     * Generic callback for admin write operations that produce no return value.
     */
    public interface AdminCallback {
        /** Called when the operation completes successfully. */
        void onSuccess();

        /**
         * Called when the operation fails.
         *
         * @param e The exception describing the failure.
         */
        void onFailure(Exception e);
    }

    /**
     * Callback for operations that return a list of {@link Student} objects.
     */
    public interface StudentListCallback {
        /**
         * Called when the student list has been successfully retrieved.
         *
         * @param students The resulting list. Never {@code null}; may be empty.
         */
        void onSuccess(List<Student> students);

        /**
         * Called when the Firestore query fails.
         *
         * @param e The exception describing the failure.
         */
        void onFailure(Exception e);
    }

    /**
     * Callback for operations that return a list of {@link Counselor} objects.
     */
    public interface CounselorListCallback {
        /**
         * Called when the counselor list has been successfully retrieved.
         *
         * @param counselors The resulting list. Never {@code null}; may be empty.
         */
        void onSuccess(List<Counselor> counselors);

        /**
         * Called when the Firestore query fails.
         *
         * @param e The exception describing the failure.
         */
        void onFailure(Exception e);
    }

    // -------------------------------------------------------------------------
    // US-18: Student account management
    // -------------------------------------------------------------------------

    /**
     * Creates a new Firebase Auth account and writes an active {@link Student} document
     * to the {@code students/} collection (US-18 criterion 1).
     *
     * <p>The new account is active by default ({@link Student#isActive()} == {@code true}).
     *
     * @param name     The student's full display name.
     * @param email    Email address for the new account.
     * @param password A temporary password; the student should change this on first login.
     * @param cb       Callback fired on write completion or failure.
     */
    public void createStudent(String name, String email, String password, AdminCallback cb) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String  uid     = result.getUser().getUid();
                    Student student = new Student(uid, name, email);
                    db.collection(COLLECTION_STUDENTS).document(uid)
                            .set(student)
                            .addOnSuccessListener(v -> {
                                activityController.logActivity("APPROVAL", "Admin created new student account: " + name, "Admin");
                                cb.onSuccess();
                            })
                            .addOnFailureListener(cb::onFailure);

                })
                .addOnFailureListener(cb::onFailure);
    }

    /**
     * Updates the display name and email of an existing student document
     * (US-18 criterion 3).
     *
     * <p>Only {@code name} and {@code email} are updated; the {@link Student#isActive()}
     * flag and all other fields are untouched. Firebase Auth email is not updated here —
     * that requires the Firebase Admin SDK and is out of scope for the mobile client.
     *
     * @param uid   Firebase UID of the student document to update.
     * @param name  New display name to store.
     * @param email New email to store in the Firestore document.
     * @param cb    Callback fired on completion or failure.
     */
    public void updateStudent(String uid, String name, String email, AdminCallback cb) {
        db.collection(COLLECTION_STUDENTS).document(uid)
                .update("name", name, "email", email)
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onFailure);
    }

    /**
     * Activates or deactivates a student account (US-18 criteria 2 and 4).
     *
     * <p>Setting {@code active = false} blocks the student at login —
     * {@code AuthController.loginUser()} reads this flag and rejects the session.
     * Setting {@code active = true} restores login access.
     *
     * <p>This is the <em>only</em> code path that can set {@code active = true}.
     * There is no student-facing reactivation route, satisfying criterion 4.
     *
     * @param uid    Firebase UID of the student document to update.
     * @param active {@code true} to activate; {@code false} to deactivate.
     * @param cb     Callback fired on completion or failure.
     */
    public void setStudentActive(String uid, boolean active, AdminCallback cb) {
        db.collection(COLLECTION_STUDENTS).document(uid)
                .update("active", active)
                .addOnSuccessListener(v -> {
                    String action = active ? "ACTIVATION" : "DEACTIVATION";
                    activityController.logActivity(action, "Admin " + (active ? "activated" : "deactivated") + " student account " + uid, "Admin");
                    cb.onSuccess();
                })
                .addOnFailureListener(cb::onFailure);

    }

    /**
     * Fetches all student documents from the {@code students/} collection,
     * including inactive accounts. Used to populate the admin's student management list.
     *
     * @param cb Delivers the full list of {@link Student} objects on success.
     */
    public void getAllStudents(StudentListCallback cb) {
        db.collection(COLLECTION_STUDENTS)
                .get()
                .addOnSuccessListener(snap -> cb.onSuccess(snap.toObjects(Student.class)))
                .addOnFailureListener(cb::onFailure);
    }

    // -------------------------------------------------------------------------
    // US-19: Counselor profile and permission management
    // -------------------------------------------------------------------------

    /**
     * Creates a new Firebase Auth account and writes an unapproved {@link Counselor}
     * document to the {@code counselors/} collection (US-19 implicit criterion).
     *
     * <p>This is the <em>only</em> registration path for counselors. The public
     * {@link RegisterActivity} has been updated to allow student self-registration only.
     * The new counselor starts with {@link Counselor#isApproved()} == {@code false}
     * and must be explicitly approved via {@link #setCounselorApproved} before appearing
     * in student-facing search results.
     *
     * @param name           The counselor's full display name.
     * @param email          Email address for the new account.
     * @param password       A temporary password for first login.
     * @param specialization Area of expertise, or {@code null} for general practice.
     * @param cb             Callback fired on write completion or failure.
     */
    public void registerCounselor(String name, String email, String password,
                                  String specialization, AdminCallback cb) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String    uid      = result.getUser().getUid();
                    Counselor counselor = new Counselor(uid, name, email, specialization, null);
                    db.collection(COLLECTION_COUNSELORS).document(uid)
                            .set(counselor)
                            .addOnSuccessListener(v -> {
                                activityController.logActivity("REGISTRATION", "Admin registered new counselor: " + name, "Admin");
                                cb.onSuccess();
                            })
                            .addOnFailureListener(cb::onFailure);

                })
                .addOnFailureListener(cb::onFailure);
    }

    /**
     * Approves or revokes admin approval for a counselor (US-19 criteria 1 and 2).
     *
     * <p>Setting {@code approved = true} makes the counselor visible in student-facing
     * search results (provided they are also not suspended). Setting {@code approved = false}
     * removes them from results immediately; existing appointments are unaffected.
     *
     * @param counselorId Firebase UID of the counselor document to update.
     * @param approved    {@code true} to approve; {@code false} to revoke.
     * @param cb          Callback fired on completion or failure.
     */
    public void setCounselorApproved(String counselorId, boolean approved, AdminCallback cb) {
        db.collection(COLLECTION_COUNSELORS).document(counselorId)
                .update("approved", approved)
                .addOnSuccessListener(v -> {
                    String action = approved ? "APPROVAL" : "REVOCATION";
                    activityController.logActivity(action, "Admin " + (approved ? "approved" : "revoked") + " counselor " + counselorId, "Admin");
                    cb.onSuccess();
                })
                .addOnFailureListener(cb::onFailure);

    }

    /**
     * Updates the name, email, and specialization of an existing counselor document
     * (US-19 criterion 2 — update counselor permissions/profile).
     *
     * <p>Approval, suspension, and rating fields are not modified here; use their
     * dedicated methods.
     *
     * @param counselorId    Firebase UID of the counselor document to update.
     * @param name           New display name.
     * @param email          New email to store in Firestore.
     * @param specialization New specialization; {@code null} for general practice.
     * @param cb             Callback fired on completion or failure.
     */
    public void updateCounselor(String counselorId, String name, String email,
                                String specialization, AdminCallback cb) {
        db.collection(COLLECTION_COUNSELORS).document(counselorId)
                .update("name", name, "email", email, "specialization", specialization)
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onFailure);
    }

    /**
     * Fetches all counselor documents from the {@code counselors/} collection,
     * including unapproved and suspended accounts.
     *
     * <p>Unlike {@code CounselorController.getAllCounselors()}, no visibility filter
     * is applied — admins must be able to see and manage counselors in every state.
     *
     * @param cb Delivers the full unfiltered list of {@link Counselor} objects on success.
     */
    public void getAllCounselors(CounselorListCallback cb) {
        db.collection(COLLECTION_COUNSELORS)
                .get()
                .addOnSuccessListener(snap -> cb.onSuccess(snap.toObjects(Counselor.class)))
                .addOnFailureListener(cb::onFailure);
    }

    // -------------------------------------------------------------------------
    // US-20: Suspension clearance after admin meeting
    // -------------------------------------------------------------------------

    /**
     * Clears a counselor's rating-triggered suspension after the admin has held
     * the required meeting (US-20 unknown 2 resolution).
     *
     * <p>This is the <em>only</em> operation that lifts a suspension set by
     * {@code ReviewController}. It performs a single atomic Firestore update that:
     * <ol>
     *   <li>Sets {@code suspended = false} — makes the counselor visible to students again.
     *   <li>Sets {@code meetingCleared = false} — resets the flag after use so the state
     *       is clean for any future suspension cycle (the flag served as a one-time
     *       admin acknowledgement and is not kept as persistent history).
     * </ol>
     *
     * <p>The counselor's {@code averageRating} and {@code reviewCount} are intentionally
     * preserved — the rating history remains on the record for transparency.
     *
     * @param counselorId Firebase UID of the suspended counselor to clear.
     * @param cb          Callback fired on completion or failure.
     */
    public void clearCounselorSuspension(String counselorId, AdminCallback cb) {
        db.collection(COLLECTION_COUNSELORS).document(counselorId)
                .update("suspended", false, "meetingCleared", false)
                .addOnSuccessListener(v -> {
                    activityController.logActivity("APPROVAL", "Admin cleared suspension for counselor " + counselorId + " after meeting", "Admin");
                    cb.onSuccess();
                })
                .addOnFailureListener(cb::onFailure);

    }
}
