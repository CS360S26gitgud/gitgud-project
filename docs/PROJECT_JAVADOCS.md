# Project Technical Documentation (User Story Mapping)

This document maps all 21 User Stories from the Product Backlog to the technical implementation in the codebase.

---

## 👨‍🎓 Student User Stories (US 01 - 11)

### US 01 & 02: Registration & Secure Login
- **Implementation**: `AuthController.java`
- **Methods**: 
    - `registerUser(String name, String email, String password, String role, AuthCallback cb)`: Securely creates a Firebase Auth account and initializes a Firestore profile.
    - `loginUser(String email, String password, AuthCallback cb)`: Authenticates credentials and redirects based on user role.

### US 03 & 10: View & Filter Available Slots
- **Implementation**: `AvailabilityController.java` & `CounselorController.java`
- **Methods**:
    - `getAllAvailableSlots(SlotListCallback cb)`: Fetches unbooked time windows across all counselors.
    - `searchCounselors(String specialization, String day, CounselorListCallback callback)`: Filters counselors by their expertise and availability.

### US 04: Book Appointment
- **Implementation**: `AppointmentController.java`
- **Method**: `bookSlot(TimeSlot slot, String studentId, BookingCallback cb)`: Uses a Firestore Transaction to ensure atomic booking (prevents double-booking).

### US 05: Cancel or Reschedule
- **Implementation**: `AppointmentController.java`
- **Methods**:
    - `cancelAppointment(String appointmentId, BookingCallback cb)`: Updates status to "CANCELLED" and releases the timeslot.
    - `rescheduleAppointment(String appointmentId, TimeSlot newSlot, BookingCallback cb)`: Atomically moves an appointment to a new time window.

### US 06, 07 & 15: Reviews & Ratings
- **Implementation**: `ReviewController.java`
- **Methods**:
    - `submitReview(Review review, ReviewCallback cb)`: Allows anonymous feedback submission.
    - `getCounselorReviews(String counselorId, ReviewListCallback cb)`: Retrieves all feedback and ratings for a specific counselor.

### US 08: Notifications
- **Implementation**: `NotificationHelper.java`
- **Method**: `sendAppointmentNotification(Context context, String title, String message)`: Uses Android NotificationManager to alert users of upcoming sessions.

### US 09: Passive Calendar Tracking
- **Implementation**: `StudentDashboardActivity.java`
- **Logic**: Integrates `CalendarView` with real-time Firestore listeners to highlight days with confirmed appointments.

### US 11: Appointment History
- **Implementation**: `AppointmentController.java`
- **Method**: `getStudentAppointmentHistory(String studentId, AppointmentListCallback callback)`: Retrieves a list of all past and present sessions for the user.

---

## 👩‍🏫 Counselor User Stories (US 12 - 17)

### US 13: Set/Update Availability
- **Implementation**: `AvailabilityController.java`
- **Methods**:
    - `addSlot(String counselorId, String date, String startTime, String endTime, AvailabilityCallback cb)`: Creates new bookable time windows.
    - `updateSlot(String slotId, ...)`: Modifies existing availability parameters.

### US 14 & 16: Manage Counselor Appointments
- **Implementation**: `AppointmentController.java`
- **Methods**:
    - `getUpcomingForCounselor(String counselorId, AppointmentListCallback cb)`: Displays the counselor's schedule.
    - `cancelAppointmentByCounselor(...)`: Allows counselors to manage conflicts.

### US 17: Relevant Materials
- **Implementation**: `AppointmentController.java`
- **Method**: `addMaterialToAppointment(String appointmentId, String materialUrl)`: Allows counselors to attach resources/links to specific sessions.

---

## 🛠️ Admin User Stories (US 18 - 21)

### US 18 & 19: User Management
- **Implementation**: `AdminController.java`
- **Methods**:
    - `createStudent(...)` / `updateStudent(...)`: Administrative CRUD operations for student accounts.
    - `manageCounselorProfile(...)`: Controls counselor status and permissions.

### US 20: Performance Thresholds
- **Implementation**: `AdminController.java`
- **Logic**: `monitorCounselorRatings()`: Automatically flags or deactivates counselors whose average rating falls below a specific threshold (e.g., 2.5 stars).

### US 21: System Activity Monitoring
- **Implementation**: `ActivityController.java`
- **Method**: `getSystemActivities(ActivityListCallback cb)`: Provides a timestamped audit log of all critical system events (logins, bookings, cancellations).
