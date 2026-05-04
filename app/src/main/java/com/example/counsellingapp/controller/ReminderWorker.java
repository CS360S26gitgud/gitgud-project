package com.example.counsellingapp.controller;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class ReminderWorker extends Worker {

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Retrieve the data passed from the controller
        String title = getInputData().getString("title");
        String message = getInputData().getString("message");

        // Use your existing NotificationHelper to show the alert
        NotificationHelper.sendNotification(getApplicationContext(), title, message);

        return Result.success();
    }
}