package com.example.counsellingapp.controller;
/**
 * Callback interface for availability (TimeSlot) Firestore operations.
 */
public interface AvailabilityCallback {
    void onSuccess();
    void onFailure(Exception e);
}