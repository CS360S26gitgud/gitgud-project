package com.example.counsellingapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for AdminController using Mockito to simulate Firestore/Auth.
 * Tests US-18, US-19, and US-20 controller logic.
 */
public class AdminControllerTest {

    private AdminController adminController;

    @Mock private FirebaseFirestore mockDb;
    @Mock private FirebaseAuth mockAuth;
    @Mock private CollectionReference mockCollection;
    @Mock private DocumentReference mockDoc;
    @Mock private Task<Void> mockVoidTask;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Make the mock task fluent to avoid NPE on chained calls
        when(mockVoidTask.addOnSuccessListener(any())).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnFailureListener(any())).thenReturn(mockVoidTask);

        // Mocking the static Firebase instances using mockito-inline
        try (MockedStatic<FirebaseFirestore> dbStatic = mockStatic(FirebaseFirestore.class);
             MockedStatic<FirebaseAuth> authStatic = mockStatic(FirebaseAuth.class)) {
            
            dbStatic.when(FirebaseFirestore::getInstance).thenReturn(mockDb);
            authStatic.when(FirebaseAuth::getInstance).thenReturn(mockAuth);
            
            when(mockDb.collection(anyString())).thenReturn(mockCollection);
            when(mockCollection.document(anyString())).thenReturn(mockDoc);
            
            adminController = new AdminController();
        }
    }

    @Test
    public void testSetStudentActive() {
        when(mockDoc.update(anyString(), any())).thenReturn(mockVoidTask);
        
        adminController.setStudentActive("test-uid", false, new AdminController.AdminCallback() {
            @Override public void onSuccess() {}
            @Override public void onFailure(Exception e) {}
        });

        // Verify that the correct collection and document were targeted
        verify(mockDb).collection("students");
        verify(mockCollection).document("test-uid");
        verify(mockDoc).update("active", false);
    }

    @Test
    public void testSetCounselorApproved() {
        when(mockDoc.update(anyString(), any())).thenReturn(mockVoidTask);
        
        adminController.setCounselorApproved("c-123", true, new AdminController.AdminCallback() {
            @Override public void onSuccess() {}
            @Override public void onFailure(Exception e) {}
        });

        verify(mockDb).collection("counselors");
        verify(mockDoc).update("approved", true);
    }

    @Test
    public void testClearSuspension() {
        when(mockDoc.update(anyString(), any(), anyString(), any())).thenReturn(mockVoidTask);
        
        adminController.clearCounselorSuspension("c-123", new AdminController.AdminCallback() {
            @Override public void onSuccess() {}
            @Override public void onFailure(Exception e) {}
        });

        verify(mockDoc).update("suspended", false, "meetingCleared", false);
    }
}

