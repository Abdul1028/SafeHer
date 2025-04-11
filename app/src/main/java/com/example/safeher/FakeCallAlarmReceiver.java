package com.example.safeher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class FakeCallAlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "FakeCallAlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Fake call alarm received. Triggering FakeCallActivity.");

        Intent fakeCallIntent = new Intent(context, FakeCallActivity.class);
        // Important: Need FLAG_ACTIVITY_NEW_TASK because we are starting from a Receiver context
        fakeCallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Optionally pass data from the original intent if needed
        // fakeCallIntent.putExtras(intent);

        try {
            context.startActivity(fakeCallIntent);
            Log.d(TAG, "FakeCallActivity started successfully.");
        } catch (Exception e) {
            // Handle cases where activity might not be found or other errors
            Log.e(TAG, "Error starting FakeCallActivity from receiver", e);
            // Maybe show a Toast or notification if feasible from receiver context?
        }
    }
}
