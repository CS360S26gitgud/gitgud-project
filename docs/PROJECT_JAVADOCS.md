# Project Javadocs Summary

This document provides a summary of the classes and methods in the Counselling App project.

## Models

### User
Represents a system user (student, counselor, or admin).
- `User()`: Default constructor.
- `User(String uid, String name, String email, String role)`: Constructs a new User.
- `getSpecialization()` / `setSpecialization(String)`: Accessors for counselor specialization.
- `getAvailableDays()` / `setAvailableDays(List<String>)`: Accessors for available days.
- `getUid()` / `setUid(String)`: Accessors for UID.
- `getName()` / `setName(String)`: Accessors for name.
- `getEmail()` / `setEmail(String)`: Accessors for email.
- `getRole()` / `setRole(String)`: Accessors for role.

### Appointment
Represents a booked session between a student and a counselor.
- `Appointment()`: Default constructor.
- `Appointment(String id, String studentId, String counselorId, String timeslotId, String status)`: Constructs a new Appointment.
- `getId()` / `setId(String)`: Accessors for appointment ID.
- `getStudentId()` / `setStudentId(String)`: Accessors for student UID.
- `getCounselorId()` / `setCounselorId(String)`: Accessors for counselor UID.
- `getTimeslotId()` / `setTimeslotId(String)`: Accessors for time slot ID.
- `getStatus()` / `setStatus(String)`: Accessors for status.
- `getStudent()` / `setStudent(User)`: Accessors for resolved student object.
- `getCounselor()` / `setCounselor(User)`: Accessors for resolved counselor object.
- `getTimeSlot()` / `setTimeSlot(TimeSlot)`: Accessors for resolved time slot object.

### TimeSlot
Represents a counselor's available date and time window.
- `TimeSlot()`: Default constructor.
- `TimeSlot(String id, String counselorId, String date, String startTime, String endTime)`: Constructs a new TimeSlot.
- `getId()` / `setId(String)`: Accessors for slot ID.
- `getDate()` / `setDate(String)`: Accessors for date.
- `getStartTime()` / `setStartTime(String)`: Accessors for start time.
- `getEndTime()` / `setEndTime(String)`: Accessors for end time.
- `isBooked()` / `setBooked(boolean)`: Accessors for booking status.

## Controllers

### AppointmentController
Manages appointment-related operations in Firestore.
- `getUpcomingForCounselor(String counselorId, AppointmentListCallback cb)`: Fetches upcoming appointments for a counselor.
- `bookSlot(TimeSlot slot, String studentId, BookingCallback cb)`: Securely books a slot using a transaction.
- `getStudentAppointmentHistory(String studentId, AppointmentListCallback callback)`: Fetches full history for a student.

### AuthController
Handles Firebase Auth and user registration.
- `registerUser(String name, String email, String password, String role, AuthCallback cb)`: Registers a new user.
- `loginUser(String email, String password, AuthCallback cb)`: Authenticates an existing user.

### AvailabilityController
Manages counselor availability slots.
- `addSlot(String counselorId, String date, String startTime, String endTime, AvailabilityCallback cb)`: Adds a new availability slot.
- `updateSlot(String slotId, String counselorId, String date, String startTime, String endTime, AvailabilityCallback cb)`: Updates an existing slot.
- `getCounselorSlots(String counselorId, SlotListCallback cb)`: Fetches unbooked slots for a counselor.
- `getAllAvailableSlots(SlotListCallback cb)`: Fetches all available slots across all counselors.

### CounselorController
Manages counselor-specific operations like searching.
- `getAllCounselors(CounselorListCallback callback)`: Fetches all counselors.
- `searchCounselors(String specialization, String day, CounselorListCallback callback)`: Filters counselors by specialization and availability.
