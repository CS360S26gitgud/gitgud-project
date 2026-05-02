package com.example.counsellingapp.view;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.counsellingapp.R;
import com.example.counsellingapp.controller.AdminController;
import com.example.counsellingapp.model.Counselor;
import com.example.counsellingapp.model.Student;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

/**
 * Main dashboard for administrators. Surfaces the operations defined in US-18, US-19,
 * and US-20 through two tabs: a student management list and a counselor management list.
 *
 * <p><b>US-18 — Student account management:</b>
 * <ul>
 *   <li>Lists all students (active and inactive).
 *   <li>"Create Student" button opens a dialog to create a new account.
 *   <li>Per-row "Edit" button opens a dialog to update name/email.
 *   <li>Per-row "Activate / Deactivate" button toggles the {@code active} flag.
 * </ul>
 *
 * <p><b>US-19 — Counselor profile and permission management:</b>
 * <ul>
 *   <li>Lists all counselors (approved, unapproved, and suspended).
 *   <li>"Register Counselor" button opens a dialog to create a new counselor account
 *       (the only registration path for counselors).
 *   <li>Per-row "Approve / Revoke" button toggles the {@code approved} flag.
 *   <li>Per-row "Edit" button opens a dialog to update name, email, and specialization.
 * </ul>
 *
 * <p><b>US-20 — Suspension clearance:</b>
 * <ul>
 *   <li>Suspended counselors are shown with a visual indicator in the counselor list.
 *   <li>Per-row "Clear Suspension" button is visible only for suspended counselors;
 *       tapping it calls {@link AdminController#clearCounselorSuspension} after a
 *       confirmation dialog confirming the meeting has been held.
 * </ul>
 *
 * <p>CRC Responsibilities:
 * <ul>
 *   <li>Present admin management UI for students and counselors.
 *   <li>Delegate all Firestore mutations to {@link AdminController}.
 *   <li>Refresh lists after each mutation via {@code onResume()}.
 * </ul>
 *
 * CRC Collaborators: {@link AdminController}, {@link Student}, {@link Counselor}
 */
public class AdminDashboardActivity extends AppCompatActivity {

    private Button      btnLogout, btnCreateStudent, btnRegisterCounselor;
    private Button      btnShowStudents, btnShowCounselors;
    private RecyclerView rvList;
    private ProgressBar  progressBar;
    private TextView     tvSectionTitle, tvEmpty;

    private AdminController adminController;

    /** Tracks which section is currently displayed: {@code "students"} or {@code "counselors"}. */
    private String currentSection = "students";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        adminController = new AdminController();

        btnLogout            = findViewById(R.id.btnAdminLogout);
        btnCreateStudent     = findViewById(R.id.btnCreateStudent);
        btnRegisterCounselor = findViewById(R.id.btnRegisterCounselor);
        btnShowStudents      = findViewById(R.id.btnShowStudents);
        btnShowCounselors    = findViewById(R.id.btnShowCounselors);
        rvList               = findViewById(R.id.rvAdminList);
        progressBar          = findViewById(R.id.progressBarAdmin);
        tvSectionTitle       = findViewById(R.id.tvAdminSectionTitle);
        tvEmpty              = findViewById(R.id.tvAdminEmpty);

        rvList.setLayoutManager(new LinearLayoutManager(this));

        btnShowStudents.setOnClickListener(v -> {
            currentSection = "students";
            loadStudents();
        });

        btnShowCounselors.setOnClickListener(v -> {
            currentSection = "counselors";
            loadCounselors();
        });

        btnCreateStudent.setOnClickListener(v -> showCreateStudentDialog());
        btnRegisterCounselor.setOnClickListener(v -> showRegisterCounselorDialog());

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LandingActivity.class));
            finish();
        });

        // Default section on open
        loadStudents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if ("counselors".equals(currentSection)) loadCounselors();
        else loadStudents();
    }

    // -------------------------------------------------------------------------
    // US-18: Student list and actions
    // -------------------------------------------------------------------------

    /**
     * Fetches all student accounts and renders them in the list.
     * Includes both active and inactive accounts (admin sees all states).
     */
    private void loadStudents() {
        tvSectionTitle.setText("Students");
        btnCreateStudent.setVisibility(View.VISIBLE);
        btnRegisterCounselor.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        adminController.getAllStudents(new AdminController.StudentListCallback() {
            @Override
            public void onSuccess(List<Student> students) {
                progressBar.setVisibility(View.GONE);
                if (students.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    rvList.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    rvList.setVisibility(View.VISIBLE);
                    rvList.setAdapter(new AdminStudentAdapter(students,
                            AdminDashboardActivity.this::showEditStudentDialog,
                            AdminDashboardActivity.this::confirmToggleStudentActive));
                }
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminDashboardActivity.this,
                        "Failed to load students: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Shows a dialog for creating a new student account (US-18 criterion 1).
     * Collects name, email, and a temporary password.
     */
    private void showCreateStudentDialog() {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_create_student, null);
        EditText etName  = dialogView.findViewById(R.id.etDialogStudentName);
        EditText etEmail = dialogView.findViewById(R.id.etDialogStudentEmail);
        EditText etPass  = dialogView.findViewById(R.id.etDialogStudentPassword);

        new AlertDialog.Builder(this)
                .setTitle("Create Student Account")
                .setView(dialogView)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name  = etName.getText().toString().trim();
                    String email = etEmail.getText().toString().trim();
                    String pass  = etPass.getText().toString().trim();
                    if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                        Toast.makeText(this, "All fields are required",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    adminController.createStudent(name, email, pass,
                            new AdminController.AdminCallback() {
                                @Override public void onSuccess() {
                                    Toast.makeText(AdminDashboardActivity.this,
                                            "Student account created",
                                            Toast.LENGTH_SHORT).show();
                                    loadStudents();
                                }
                                @Override public void onFailure(Exception e) {
                                    Toast.makeText(AdminDashboardActivity.this,
                                            "Failed: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Shows a dialog for editing an existing student's name and email (US-18 criterion 3).
     *
     * @param student The {@link Student} whose details are being updated.
     */
    private void showEditStudentDialog(Student student) {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_create_student, null);
        EditText etName  = dialogView.findViewById(R.id.etDialogStudentName);
        EditText etEmail = dialogView.findViewById(R.id.etDialogStudentEmail);
        EditText etPass  = dialogView.findViewById(R.id.etDialogStudentPassword);

        etName.setText(student.getName());
        etEmail.setText(student.getEmail());
        etPass.setVisibility(View.GONE); // Password cannot be updated from the client

        new AlertDialog.Builder(this)
                .setTitle("Edit Student")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name  = etName.getText().toString().trim();
                    String email = etEmail.getText().toString().trim();
                    if (name.isEmpty() || email.isEmpty()) {
                        Toast.makeText(this, "Name and email are required",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    adminController.updateStudent(student.getUid(), name, email,
                            new AdminController.AdminCallback() {
                                @Override public void onSuccess() {
                                    Toast.makeText(AdminDashboardActivity.this,
                                            "Student updated", Toast.LENGTH_SHORT).show();
                                    loadStudents();
                                }
                                @Override public void onFailure(Exception e) {
                                    Toast.makeText(AdminDashboardActivity.this,
                                            "Failed: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Shows a confirmation dialog before toggling a student's active state
     * (US-18 criteria 2 and 4).
     *
     * @param student The {@link Student} whose active flag is being toggled.
     */
    private void confirmToggleStudentActive(Student student) {
        boolean willDeactivate = student.isActive();
        String  action         = willDeactivate ? "deactivate" : "reactivate";

        new AlertDialog.Builder(this)
                .setTitle((willDeactivate ? "Deactivate" : "Reactivate") + " Account")
                .setMessage("Are you sure you want to " + action + " "
                        + student.getName() + "'s account?")
                .setPositiveButton("Yes", (dialog, which) ->
                        adminController.setStudentActive(
                                student.getUid(), !student.isActive(),
                                new AdminController.AdminCallback() {
                                    @Override public void onSuccess() {
                                        Toast.makeText(AdminDashboardActivity.this,
                                                "Account " + action + "d",
                                                Toast.LENGTH_SHORT).show();
                                        loadStudents();
                                    }
                                    @Override public void onFailure(Exception e) {
                                        Toast.makeText(AdminDashboardActivity.this,
                                                "Failed: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    }
                                }))
                .setNegativeButton("No", null)
                .show();
    }

    // -------------------------------------------------------------------------
    // US-19: Counselor list and actions
    // -------------------------------------------------------------------------

    /**
     * Fetches all counselor accounts (including unapproved and suspended) and
     * renders them in the list.
     */
    private void loadCounselors() {
        tvSectionTitle.setText("Counselors");
        btnCreateStudent.setVisibility(View.GONE);
        btnRegisterCounselor.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        adminController.getAllCounselors(new AdminController.CounselorListCallback() {
            @Override
            public void onSuccess(List<Counselor> counselors) {
                progressBar.setVisibility(View.GONE);
                if (counselors.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    rvList.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    rvList.setVisibility(View.VISIBLE);
                    rvList.setAdapter(new AdminCounselorAdapter(
                            counselors,
                            AdminDashboardActivity.this::showEditCounselorDialog,
                            AdminDashboardActivity.this::confirmToggleCounselorApproval,
                            AdminDashboardActivity.this::confirmClearSuspension));
                }
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminDashboardActivity.this,
                        "Failed to load counselors: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Shows a dialog for registering a new counselor account (US-19).
     * This is the only registration path for counselors.
     */
    private void showRegisterCounselorDialog() {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_register_counselor, null);
        EditText etName  = dialogView.findViewById(R.id.etDialogCounselorName);
        EditText etEmail = dialogView.findViewById(R.id.etDialogCounselorEmail);
        EditText etPass  = dialogView.findViewById(R.id.etDialogCounselorPassword);
        EditText etSpec  = dialogView.findViewById(R.id.etDialogCounselorSpec);

        new AlertDialog.Builder(this)
                .setTitle("Register Counselor")
                .setView(dialogView)
                .setPositiveButton("Register", (dialog, which) -> {
                    String name  = etName.getText().toString().trim();
                    String email = etEmail.getText().toString().trim();
                    String pass  = etPass.getText().toString().trim();
                    String spec  = etSpec.getText().toString().trim();
                    if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                        Toast.makeText(this, "Name, email and password are required",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    adminController.registerCounselor(name, email, pass,
                            spec.isEmpty() ? null : spec,
                            new AdminController.AdminCallback() {
                                @Override public void onSuccess() {
                                    Toast.makeText(AdminDashboardActivity.this,
                                            "Counselor registered — pending approval",
                                            Toast.LENGTH_SHORT).show();
                                    loadCounselors();
                                }
                                @Override public void onFailure(Exception e) {
                                    Toast.makeText(AdminDashboardActivity.this,
                                            "Failed: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Shows a dialog for editing a counselor's name, email, and specialization
     * (US-19 criterion 2).
     *
     * @param counselor The {@link Counselor} whose profile is being updated.
     */
    private void showEditCounselorDialog(Counselor counselor) {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_register_counselor, null);
        EditText etName  = dialogView.findViewById(R.id.etDialogCounselorName);
        EditText etEmail = dialogView.findViewById(R.id.etDialogCounselorEmail);
        EditText etPass  = dialogView.findViewById(R.id.etDialogCounselorPassword);
        EditText etSpec  = dialogView.findViewById(R.id.etDialogCounselorSpec);

        etName.setText(counselor.getName());
        etEmail.setText(counselor.getEmail());
        etPass.setVisibility(View.GONE);
        if (counselor.getSpecialization() != null) etSpec.setText(counselor.getSpecialization());

        new AlertDialog.Builder(this)
                .setTitle("Edit Counselor")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name  = etName.getText().toString().trim();
                    String email = etEmail.getText().toString().trim();
                    String spec  = etSpec.getText().toString().trim();
                    if (name.isEmpty() || email.isEmpty()) {
                        Toast.makeText(this, "Name and email are required",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    adminController.updateCounselor(counselor.getUid(), name, email,
                            spec.isEmpty() ? null : spec,
                            new AdminController.AdminCallback() {
                                @Override public void onSuccess() {
                                    Toast.makeText(AdminDashboardActivity.this,
                                            "Counselor updated", Toast.LENGTH_SHORT).show();
                                    loadCounselors();
                                }
                                @Override public void onFailure(Exception e) {
                                    Toast.makeText(AdminDashboardActivity.this,
                                            "Failed: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Shows a confirmation dialog before toggling a counselor's approval state
     * (US-19 criteria 1 and 2).
     *
     * @param counselor The {@link Counselor} whose approval is being toggled.
     */
    private void confirmToggleCounselorApproval(Counselor counselor) {
        boolean willRevoke = counselor.isApproved();
        String  action     = willRevoke ? "revoke approval for" : "approve";

        new AlertDialog.Builder(this)
                .setTitle(willRevoke ? "Revoke Approval" : "Approve Counselor")
                .setMessage("Are you sure you want to " + action
                        + " " + counselor.getName() + "?")
                .setPositiveButton("Yes", (dialog, which) ->
                        adminController.setCounselorApproved(
                                counselor.getUid(), !counselor.isApproved(),
                                new AdminController.AdminCallback() {
                                    @Override public void onSuccess() {
                                        Toast.makeText(AdminDashboardActivity.this,
                                                "Approval status updated",
                                                Toast.LENGTH_SHORT).show();
                                        loadCounselors();
                                    }
                                    @Override public void onFailure(Exception e) {
                                        Toast.makeText(AdminDashboardActivity.this,
                                                "Failed: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    }
                                }))
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Shows a confirmation dialog before clearing a counselor's rating-triggered
     * suspension (US-20). The dialog text reinforces that the admin should only
     * confirm this after the required meeting has been held.
     *
     * @param counselor The suspended {@link Counselor} being cleared.
     */
    private void confirmClearSuspension(Counselor counselor) {
        new AlertDialog.Builder(this)
                .setTitle("Clear Suspension")
                .setMessage("Confirm that a meeting has been held with "
                        + counselor.getName()
                        + " and they are cleared to return. "
                        + "Current average rating: "
                        + String.format("%.1f", counselor.getAverageRating()) + " / 5.0")
                .setPositiveButton("Confirm & Clear", (dialog, which) ->
                        adminController.clearCounselorSuspension(
                                counselor.getUid(),
                                new AdminController.AdminCallback() {
                                    @Override public void onSuccess() {
                                        Toast.makeText(AdminDashboardActivity.this,
                                                counselor.getName()
                                                        + " suspension cleared",
                                                Toast.LENGTH_SHORT).show();
                                        loadCounselors();
                                    }
                                    @Override public void onFailure(Exception e) {
                                        Toast.makeText(AdminDashboardActivity.this,
                                                "Failed: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    }
                                }))
                .setNegativeButton("Cancel", null)
                .show();
    }
}
