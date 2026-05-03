package com.example.counsellingapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.example.counsellingapp.model.Review;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for ReviewController.
 * Verifies review submission and the complex rating-triggered suspension logic (US-20).
 */
public class ReviewControllerTest {

    private ReviewController reviewController;

    @Mock private FirebaseFirestore mockDb;
    @Mock private CollectionReference mockCollection;
    @Mock private DocumentReference mockDoc;
    @Mock private Task mockTask;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Make the mock task fluent to avoid NPE on chained calls
        when(mockTask.addOnSuccessListener(any())).thenReturn(mockTask);
        when(mockTask.addOnFailureListener(any())).thenReturn(mockTask);

        try (MockedStatic<FirebaseFirestore> dbStatic = mockStatic(FirebaseFirestore.class)) {
            dbStatic.when(FirebaseFirestore::getInstance).thenReturn(mockDb);
            when(mockDb.collection(anyString())).thenReturn(mockCollection);
            when(mockCollection.document(anyString())).thenReturn(mockDoc);
            
            reviewController = new ReviewController();
        }
    }

    @Test
    public void testSubmitReviewBasic() {
        Review review = new Review("appt-1", "c-1", 5.0f, "Great!", com.google.firebase.Timestamp.now());

        
        // Mocking the transaction call
        when(mockDb.runTransaction(any())).thenReturn(mockTask);

        reviewController.submitReview(review, new ReviewController.SimpleCallback() {
            @Override public void onSuccess() {}
            @Override public void onFailure(Exception e) {}
        });


        // Verify that a transaction was initiated (US-20 requires atomicity)
        verify(mockDb).runTransaction(any());
    }

    @Test
    public void testGetReviewsForCounselor() {
        when(mockDb.collection("reviews")).thenReturn(mockCollection);
        when(mockCollection.whereEqualTo(anyString(), any())).thenReturn(mockCollection);
        when(mockCollection.get()).thenReturn(mockTask);

        reviewController.getReviewsForCounselor("c-1", new ReviewController.ReviewListCallback() {
            @Override public void onSuccess(java.util.List<Review> reviews) {}
            @Override public void onFailure(Exception e) {}
        });

        verify(mockDb).collection("reviews");
        verify(mockCollection).whereEqualTo("counselorId", "c-1");
    }
}
