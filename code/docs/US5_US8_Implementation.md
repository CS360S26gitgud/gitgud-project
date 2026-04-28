# Implementation Documentation: Appointment Management & Notifications

This document combines the implementation details for **User Story 5 (Cancel / Reschedule Appointment)** and **User Story 8 (Appointment Notifications)**.

---

## 1. Overview
These features provide students with full control over their scheduled sessions and ensure they receive immediate feedback via system notifications when changes occur.

### Fulfillment:
- **US5**: Allows students to cancel or move upcoming appointments, ensuring counselors' time is freed up and students can manage their schedules.
- **US8**: Alerts students through Android system notifications whenever an appointment is booked or rescheduled.

---

## 2. Changes & Files Touched

### A. Model & Controller Layer
- **`controller/NotificationHelper.java` (NEW)**:
    - Added to handle the creation of `NotificationChannel` and dispatching local notifications.
- **`controller/AppointmentController.java` (UPDATED)**:
    - Added `cancelAppointment`: Uses Firestore Transactions to atomically update appointment status to "cancelled" and set the TimeSlot to "booked: false".
    - Added `rescheduleAppointment`: Uses Firestore Transactions to swap TimeSlots, ensuring the old one is freed and the new one is reserved only if available.
    - Integrated `NotificationHelper`: Called inside `bookSlot` and `rescheduleAppointment` success callbacks.

### B. View & Adapter Layer
- **`view/HistoryAdapter.java` (UPDATED)**:
    - Added `OnAppointmentInteractionListener` interface.
    - Added logic to show "Cancel" and "Reschedule" buttons only for "upcoming" appointments.
- **`view/AppointmentHistoryActivity.java` (UPDATED)**:
    - Implemented the interaction listener to trigger confirmation and selection dialogs.
    - Added logic to fetch all available slots for rescheduling selection.
- **`view/AvailableSlotsActivity.java` (UPDATED)**:
    - Updated `bookSlot` call to pass `Activity` context for notification support.
- **`layout/item_history_appointment.xml` (UPDATED)**:
    - Added the "Cancel" and "Reschedule" buttons to the history item UI.

---

## 3. How to Navigate & Test

### Navigation:
1. **Login** as a Student.
2. **To Book**: Go to **Available Slots** from the Dashboard. Select a slot and click "Yes".
3. **To Manage**: Go to **Appointment History** from the Dashboard. All upcoming sessions will show "Cancel" and "Reschedule" buttons.

### Testing Output:
- **Notifications**: Immediately after booking or rescheduling, a notification appears in the Android system tray with details (Counselor name, Date, Time).
- **Cancellation**: The appointment status changes to "cancelled" and the counselor's slot becomes visible again in "Available Slots".
- **Rescheduling**: The appointment updates to the new time, and the previous slot is automatically released back into the pool.
- **Concurrency**: If two users try to take the same slot during a reschedule, the transaction logic ensures only one succeeds, showing an error to the other.
