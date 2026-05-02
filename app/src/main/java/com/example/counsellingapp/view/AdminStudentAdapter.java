package com.example.counsellingapp.view;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.counsellingapp.R;
import com.example.counsellingapp.model.Student;

import java.util.List;

/**
 * RecyclerView adapter for the student management list inside {@link AdminDashboardActivity}.
 *
 * <p>Each card displays the student's name, email, and current active state, with
 * "Edit" and "Activate / Deactivate" action buttons.
 *
 * <p>Inactive student rows are visually distinguished by rendering the name in red,
 * making it easy for the admin to identify deactivated accounts at a glance (US-18).
 *
 * <p>CRC Responsibilities:
 * <ul>
 *   <li>Bind {@link Student} data to admin management cards.
 *   <li>Fire edit and toggle-active callbacks to {@link AdminDashboardActivity}.
 * </ul>
 *
 * CRC Collaborators: {@link Student}, {@link AdminDashboardActivity}
 */
public class AdminStudentAdapter
        extends RecyclerView.Adapter<AdminStudentAdapter.ViewHolder> {

    /**
     * Listener for edit actions on a student row.
     */
    public interface OnEditStudentListener {
        /**
         * Called when the admin taps the Edit button for a student.
         *
         * @param student The {@link Student} to edit.
         */
        void onEdit(Student student);
    }

    /**
     * Listener for activate/deactivate toggle actions on a student row.
     */
    public interface OnToggleActiveListener {
        /**
         * Called when the admin taps the Activate/Deactivate button for a student.
         *
         * @param student The {@link Student} whose active flag is being toggled.
         */
        void onToggleActive(Student student);
    }

    private final List<Student>          students;
    private final OnEditStudentListener  editListener;
    private final OnToggleActiveListener toggleListener;

    /**
     * Constructs a new {@code AdminStudentAdapter}.
     *
     * @param students       The list of {@link Student} objects to display.
     * @param editListener   Callback fired when the Edit button is tapped.
     * @param toggleListener Callback fired when the Activate/Deactivate button is tapped.
     */
    public AdminStudentAdapter(List<Student> students,
                               OnEditStudentListener editListener,
                               OnToggleActiveListener toggleListener) {
        this.students       = students;
        this.editListener   = editListener;
        this.toggleListener = toggleListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_student, parent, false);
        return new ViewHolder(v);
    }

    /**
     * Binds a {@link Student} to a management card at the given position.
     *
     * <p>The student's name is rendered in red if the account is inactive, and in
     * the default text color otherwise. The toggle button label changes between
     * "Deactivate" and "Reactivate" to reflect the current state.
     *
     * @param holder   The {@link ViewHolder} to populate.
     * @param position Zero-based position in the adapter list.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Student student = students.get(position);

        holder.tvName.setText(student.getName() != null ? student.getName() : "—");
        holder.tvEmail.setText(student.getEmail() != null ? student.getEmail() : "—");

        if (student.isActive()) {
            holder.tvStatus.setText("Active");
            holder.tvStatus.setTextColor(Color.parseColor("#4CAF50"));
            holder.btnToggleActive.setText("Deactivate");
        } else {
            holder.tvStatus.setText("Inactive");
            holder.tvStatus.setTextColor(Color.RED);
            holder.btnToggleActive.setText("Reactivate");
        }

        holder.btnEdit.setOnClickListener(v -> editListener.onEdit(student));
        holder.btnToggleActive.setOnClickListener(v -> toggleListener.onToggleActive(student));
    }

    /** @return The number of students in the adapter list. */
    @Override
    public int getItemCount() { return students.size(); }

    /**
     * ViewHolder for a single student management card.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvStatus;
        Button   btnEdit, btnToggleActive;

        /**
         * Constructs a new {@code ViewHolder} and binds its child views.
         *
         * @param itemView The inflated item view for a single student card.
         */
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName         = itemView.findViewById(R.id.tvAdminStudentName);
            tvEmail        = itemView.findViewById(R.id.tvAdminStudentEmail);
            tvStatus       = itemView.findViewById(R.id.tvAdminStudentStatus);
            btnEdit        = itemView.findViewById(R.id.btnAdminEditStudent);
            btnToggleActive = itemView.findViewById(R.id.btnAdminToggleStudentActive);
        }
    }
}
