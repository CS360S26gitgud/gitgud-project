# Implementation Documentation: User Stories 10 & 11

This document outlines the changes and additions made to implement **User Story 10** (Counselor Search/Filter) and **User Story 11** (Appointment History) within the CounsellingApp.

---

## 1. User Story 10: Search/Filter Counselors
**Requirement:** As a student, I want to be able to search/filter through the available list of counselors by their specializations or availability.

### How it was implemented:
- **Model Extension:** Updated `User.java` to include `specialization` (String) and `availableDays` (List<String>). This allows Firestore to store and retrieve these attributes for users with the "counselor" role.
- **Backend Logic (`CounselorController.java`):**
    - Created `searchCounselors(specialization, day, callback)`.
    - It queries the `users` collection in Firestore, filtering by `role == "counselor"`.
    - If a specialization is provided, it adds a Firestore `whereEqualTo` filter.
    - Since Firestore has limitations with complex array queries without predefined indexes, the "Available Day" filter is applied in-memory after fetching the results to ensure reliability for the prototype.
- **UI (`CounselorSearchActivity.java` & `activity_counselor_search.xml`):**
    - Provides input fields for Specialization and Day.
    - Uses a `ListView` to display results dynamically.
    - Clicking "Filter" triggers the controller and refreshes the list.

---

## 2. User Story 11: Appointment History
**Requirement:** As a student, I want to view my appointment history so that I can keep track of past sessions.

### How it was implemented:
- **New Model (`Appointment.java`):** 
    - Created to map the `appointments` collection in Firestore.
    - Fields: `id`, `studentId`, `counselorId`, `counselorName`, `dateTime` (Timestamp), and `status`.
- **Backend Logic (`AppointmentController.java`):**
    - Created `getStudentAppointmentHistory(studentId, callback)`.
    - It queries the `appointments` collection where `studentId` matches the currently logged-in user.
    - It uses `orderBy("dateTime", Query.Direction.DESCENDING)` to show the most recent appointments first.
- **UI (`AppointmentHistoryActivity.java` & `activity_appointment_history.xml`):**
    - Automatically fetches history when the screen opens using the current Firebase User ID.
    - Formats the Firebase `Timestamp` into a readable date string (e.g., "15 Oct 2023, 14:00").
    - Displays details in a `ListView`.

---

## 3. Summary of New & Modified Files

### New Files Created:
| File Path | Purpose |
|-----------|---------|
| `model/Appointment.java` | Data class for Firestore appointment documents. |
| `controller/CounselorController.java` | Handles search and filtering logic for counselors. |
| `controller/AppointmentController.java` | Handles fetching appointment records from Firestore. |
| `view/CounselorSearchActivity.java` | UI logic for the counselor search screen. |
| `view/AppointmentHistoryActivity.java` | UI logic for the appointment history screen. |
| `layout/activity_counselor_search.xml` | Layout for the search screen. |
| `layout/activity_appointment_history.xml` | Layout for the history screen. |

### Modified Files:
| File Path | Changes Made |
|-----------|--------------|
| `model/User.java` | Added `specialization` and `availableDays` fields. |
| `view/StudentDashboardActivity.java` | Added Intent triggers for the two new activities. |
| `layout/activity_student_dashboard.xml` | Added "Search Counselors" and "Appointment History" buttons. |
| `AndroidManifest.xml` | Registered the two new Activities so they can be launched. |
| `README.md` | Updated the Product Backlog status to "Done" for US 10 & 11. |

---

## 4. Flow of Data
1. **User Logs In** (Existing US 1/2 logic).
2. **Dashboard:** Student selects either "Search" or "History".
3. **Activity Launch:** The UI calls the respective **Controller**.
4. **Firestore Fetch:** The Controller performs an asynchronous query to the Firebase database.
5. **Callback:** Once data arrives, the Controller passes a List of Objects (`User` or `Appointment`) back to the Activity.
6. **UI Update:** The Activity updates the `ListView` adapter, displaying the data to the student.
