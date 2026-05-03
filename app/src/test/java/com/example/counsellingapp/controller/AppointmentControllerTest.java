package com.example.counsellingapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.example.counsellingapp.model.Appointment;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for AppointmentController.
 * Covers US-16 (Cancellation/Rescheduling) and US-17 (Materials) logic.
 */
public class AppointmentControllerTest {

    private AppointmentController appointmentController;

    @Mock private FirebaseFirestore mockDb;
    @Mock private CollectionReference mockCollection;
    @Mock private DocumentReference mockDoc;
    @Mock private com.google.android.gms.tasks.Task<Void> mockVoidTask;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Make the mock task fluent to avoid NPE on chained calls
        when(mockVoidTask.addOnSuccessListener(any())).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnFailureListener(any())).thenReturn(mockVoidTask);

        try (MockedStatic<FirebaseFirestore> dbStatic = mockStatic(FirebaseFirestore.class)) {
            dbStatic.when(FirebaseFirestore::getInstance).thenReturn(mockDb);
            when(mockDb.collection(anyString())).thenReturn(mockCollection);
            when(mockCollection.document(anyString())).thenReturn(mockDoc);
            
            appointmentController = new AppointmentController();
        }
    }


    @Test
    public void testMarkAsCompleted() {
        when(mockDoc.update(anyString(), any())).thenReturn(mockVoidTask);

        appointmentController.markAsCompleted("appt-123", new AppointmentController.BookingCallback() {
            @Override public void onSuccess() {}
            @Override public void onFailure(Exception e) {}
        });

        verify(mockDb).collection("appointments");
        verify(mockDoc).update("status", "completed");
    }

    @Test
    public void testAddMaterials() {
        when(mockDoc.update(anyString(), any())).thenReturn(mockVoidTask);
        java.util.List<String> materials = java.util.Collections.singletonList("http://test.link");

        appointmentController.addMaterialToAppointment("appt-123", materials, 
            new AppointmentController.BookingCallback() {
                @Override public void onSuccess() {}
                @Override public void onFailure(Exception e) {}
            });

        // Verify US-17 material update
        verify(mockDoc).update("materials", materials);
    }
}


