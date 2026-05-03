package com.example.counsellingapp.controller;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;

/**
 * Helper class to manage local notifications for appointments (US-08).
 * Provides a unified way to notify students and counselors about bookings,
 * cancellations, and rescheduling events.
 */
public class NotificationHelper {

    private static final String CHANNEL_ID = "appointment_channel";
    private static final String CHANNEL_NAME = "Appointment Notifications";

    /**
     * Shows a local notification for an appointment event.
     *
     * @param context The application or activity context.
     * @param title   The title of the notification.
     * @param message The body text of the notification.
     */
    public static void sendNotification(Context context, String title, String message) {
        NotificationManager notificationManager = (NotificationManager) 
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // System default icon
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        // Notify using a unique ID (timestamp)
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
