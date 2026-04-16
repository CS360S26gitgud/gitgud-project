package com.example.counsellingapp.controller;

/**
 * Callback interface for Firebase Auth operations.
 * Used to communicate success or failure back to the calling Activity.
 */
public interface AuthCallback {
    void onSuccess();
    void onFailure(Exception e);
}
