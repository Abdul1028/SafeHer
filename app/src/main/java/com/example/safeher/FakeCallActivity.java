package com.example.safeher;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager; // Import if needed for volume controls later
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager; // For newer Vibration APIs
import android.util.Log;
import android.view.View;
import android.view.WindowManager; // For lock screen flags
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity; // Use AppCompatActivity

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;

public class FakeCallActivity extends AppCompatActivity {

    private static final String TAG = "FakeCallActivity";
    private static final long CALL_DURATION_MS = 20000; // Stop after 20 seconds
    private static final long[] VIBRATION_PATTERN = {0, 1000, 1000}; // 0ms delay, 1s vibrate, 1s pause
    private static final String PREFS_NAME = "SOSSettings"; // Same prefs name used in Settings
    private static final String FAKE_CALLER_NAME_KEY = "fakeCallerName"; // Same key used in Settings

    private Ringtone ringtone;
    private Vibrator vibrator;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable stopCallRunnable;

    private ShapeableImageView callerImageView;
    private TextView callerNameTextView;
    private FloatingActionButton declineButton;
    private FloatingActionButton answerButton; // Only for visual, no action needed

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate started");

        // Flags to show activity over lock screen and keep screen on
        // Important for incoming call simulation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            // Consider adding KeyguardManager dismissal if needed, but requires more permissions/logic
        } else {
            // Deprecated flags but necessary for older versions
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD // Be careful with this one
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        setContentView(R.layout.activity_fake_call);

        callerImageView = findViewById(R.id.callerImageView);
        callerNameTextView = findViewById(R.id.callerNameTextView);
        declineButton = findViewById(R.id.declineButton);
        answerButton = findViewById(R.id.answerButton);

        // Load and display the caller name from SharedPreferences
        loadCallerDetails();

        declineButton.setOnClickListener(v -> stopCallAndFinish());
        // No essential action needed for answer button in this simple version
        answerButton.setOnClickListener(v -> {
             Toast.makeText(this, "Answering... (Simulation)", Toast.LENGTH_SHORT).show();
            // In a more complex version, you might change the UI (e.g., hide buttons, show timer)
            stopRingtoneAndVibration(); // Stop ringing/vibrating if "answered"
            // Keep the activity open until the handler finishes it or user explicitly ends
        });

        startRingtoneAndVibration();

        // Schedule the call to stop automatically after a duration
        stopCallRunnable = this::stopCallAndFinish;
        handler.postDelayed(stopCallRunnable, CALL_DURATION_MS);
        Log.d(TAG, "Call scheduled to stop in " + CALL_DURATION_MS + "ms");
    }

    private void loadCallerDetails() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String callerName = prefs.getString(FAKE_CALLER_NAME_KEY, "Mom"); // Default to "Mom"
        callerNameTextView.setText(callerName);
        Log.d(TAG, "Loaded fake caller name: " + callerName);

        // TODO: Load caller image later if implemented
        // String imageUriString = prefs.getString(FAKE_CALLER_IMAGE_KEY, null);
        // if (imageUriString != null) { ... load image into callerImageView ... }
    }

    private void startRingtoneAndVibration() {
        Log.d(TAG, "Starting ringtone and vibration");
        // Start Ringtone
        try {
            Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            if (ringtoneUri != null) {
                ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
                if (ringtone != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ringtone.setLooping(true); // Ensure looping on newer devices
                    }
                    ringtone.play();
                    Log.d(TAG, "Ringtone playing");
                } else {
                    Log.w(TAG, "Could not get Ringtone object from URI.");
                }
            } else {
                 Log.w(TAG, "Could not get default ringtone URI.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing ringtone", e);
        }

        // Start Vibration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if (vm != null) {
                vibrator = vm.getDefaultVibrator();
            }
        } else {
            // Deprecated in API 31
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }

        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Repeat vibration pattern indefinitely (index 0)
                vibrator.vibrate(VibrationEffect.createWaveform(VIBRATION_PATTERN, 0));
                 Log.d(TAG, "Vibration started (Oreo+)");
            } else {
                // Deprecated in API 26
                vibrator.vibrate(VIBRATION_PATTERN, 0); // Repeat at index 0
                 Log.d(TAG, "Vibration started (pre-Oreo)");
            }
        } else {
            Log.w(TAG, "Device does not have a vibrator or service not found.");
        }
    }

    private void stopRingtoneAndVibration() {
        Log.d(TAG, "Stopping ringtone and vibration");
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
            Log.d(TAG, "Ringtone stopped");
        }
        if (vibrator != null) {
            vibrator.cancel(); // Stop vibration
            Log.d(TAG, "Vibration cancelled");
        }
    }

    // Call this when the user "declines" or the timer runs out
    private void stopCallAndFinish() {
        Log.d(TAG, "Stopping fake call and finishing activity");
        stopRingtoneAndVibration();
        handler.removeCallbacks(stopCallRunnable); // Prevent the delayed runnable from firing if already stopped
        finish(); // Close the activity
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");
        // Ensure resources are released when activity is destroyed
        stopRingtoneAndVibration();
        handler.removeCallbacks(stopCallRunnable); // Clean up handler
    }
}
