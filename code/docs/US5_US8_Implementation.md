# Implementation Documentation: Appointment Management & Notifications

This document combines the implementation details for **User Story 5 (Cancel / Reschedule Appointment)** and **User Story 8 (Appointment Notifications)**, covering both **Student** and **Counselor** perspectives.

---

## 1. Overview
These features provide both students and counselors with full control over their scheduled sessions and ensure immediate feedback via system notifications when changes occur.

### Fulfillment:
- **US5 (Student & Counselor)**: Allows both parties to cancel or move upcoming appointments. Counselor-side changes automatically notify the student.
- **US8**: Alerts students through Android system notifications whenever an appointment is booked, rescheduled, or cancelled by a counselor.

---

## 2. Changes & Files Touched

### A. Model & Controller Layer
- **`controller/NotificationHelper.java` (NEW)**:
    - Utility to handle `NotificationChannel` (Android 8.0+) and dispatching local notifications.
- **`controller/AppointmentController.java` (UPDATED)**:
    - `cancelAppointment`: Student-driven cancellation.
    - `cancelAppointmentByCounselor`: Counselor-driven cancellation with student notification.
    - `rescheduleAppointment`: Student-driven reschedule.
    - `rescheduleAppointmentByCounselor`: Counselor-driven reschedule with student notification.
    - All methods use **Firestore Transactions** to ensure atomic updates between Appointment and TimeSlot documents.

### B. View & Adapter Layer
- **`view/HistoryAdapter.java` & `view/AppointmentHistoryActivity.java`**:
    - Student-side UI for managing history and triggering actions.
- **`view/AppointmentAdapter.java` & `view/CounselorDashboardActivity.java`**:
    - Counselor-side UI. Added "Cancel" and "Reschedule" buttons to the appointment cards and implemented the dialog logic.
- **`layout/item_history_appointment.xml` & `layout/item_appointment.xml`**:
    - Added UI buttons for management actions.

---

## 3. How to Navigate & Test

### Student Perspective:
1. **Login** as a Student.
2. **Book**: Navigate to **Available Slots** and book a session.
3. **Manage**: Go to **Appointment History**. Use "Cancel" or "Reschedule" buttons.
4. **Verification**: Observe status changes in the list and system notifications for each action.

### Counselor Perspective:
1. **Login** as a Counselor.
2. **Manage**: On the **Dashboard**, upcoming appointments now show "Cancel" and "Reschedule" buttons.
3. **Cancel**: Click "Cancel" -> Confirm. The student will receive a notification.
4. **Reschedule**: Click "Reschedule" -> Select a new available slot. The appointment updates and the student is notified of the new time.

### Technical Verification:
- **Atomic State**: In Firestore, verify that `booked` status of slots always stays in sync with `status` of appointments.
- **Notifications**: Check the Android system tray (pull down from top) to see alerts for "Appointment Booked", "Appointment Cancelled", and "Appointment Rescheduled".
