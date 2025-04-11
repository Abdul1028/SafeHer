package com.example.safeher;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class AlarmSchedulerHelper {

    private static final String TAG = "AlarmSchedulerHelper";

    public static void scheduleFakeCall(Context context, long intervalMs) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(context, FakeCallAlarmReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, flags);

        long triggerAtMillis = System.currentTimeMillis() + intervalMs;

        if (alarmManager != null) {
            try {
                // Check for exact alarm permission on Android S+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (!alarmManager.canScheduleExactAlarms()) {
                        Log.w(TAG, "Exact alarm permission not granted. Cannot schedule fake call precisely.");
                        // Optionally, notify the user or request permission here
                        return; // Exit if permission is missing
                    }
                }
                // Schedule the exact alarm
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                Log.i(TAG, "Scheduled fake call alarm for " + intervalMs + "ms from now.");
            } catch (SecurityException se) {
                Log.e(TAG, "SecurityException scheduling exact alarm.", se);
                // Handle the case where permission might have been revoked between check and set
            }
        } else {
            Log.e(TAG, "AlarmManager is null. Cannot schedule fake call.");
        }
    }

    public static void cancelFakeCall(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(context, FakeCallAlarmReceiver.class);
        int flags = PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE; // Check if it exists
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, flags);

        if (alarmManager != null && pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel(); // Also cancel the PendingIntent itself
            Log.i(TAG, "Cancelled scheduled fake call alarm.");
        } else {
            Log.d(TAG, "No scheduled fake call alarm found to cancel.");
        }
    }
}
