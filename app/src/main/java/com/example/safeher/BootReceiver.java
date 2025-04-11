package com.example.safeher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";
    private static final String PREFS_NAME = "SOSSettings";
    private static final String FAKE_CALL_SCHEDULE_ENABLED_KEY = "fakeCallScheduleEnabled";
    private static final String FAKE_CALL_INTERVAL_MS_KEY = "fakeCallIntervalMs";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.i(TAG, "Device booted. Checking for scheduled fake call.");

            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            boolean scheduleEnabled = prefs.getBoolean(FAKE_CALL_SCHEDULE_ENABLED_KEY, false);
            long intervalMs = prefs.getLong(FAKE_CALL_INTERVAL_MS_KEY, 0);

            if (scheduleEnabled && intervalMs > 0) {
                Log.d(TAG, "Fake call was scheduled. Re-scheduling alarm using AlarmSchedulerHelper.");
                // Use the helper to re-schedule the alarm
                AlarmSchedulerHelper.scheduleFakeCall(context, intervalMs);
            } else {
                Log.d(TAG, "Fake call was not scheduled or interval was invalid on boot.");
            }
        }
    }
} 