package com.example.safeher;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Toast;
import android.view.KeyEvent;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.Manifest;


public class DashBoardActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 100;
    private static final String PHONE_NUMBER = "9594693842";
    private static final String HELP_MESSAGE = "Help me! I need assistance.";

    private static final int MULTI_PRESS_INTERVAL = 600; // Time interval for triple press detection (600ms)
    private long firstPressTime = 0;
    private int pressCount = 0;
    private int lastKeyCode = 0;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dash_board);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        loadFragment(new HomeFragment());

        bottomNav.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId(); // Get the selected item ID

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            } else if (itemId == R.id.nav_settings) {
                selectedFragment = new SettingsFragment();
            }

            // Load the selected fragment
            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }
            return true;
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
        }
        FloatingActionButton sosButton = findViewById(R.id.sosButton);
        sosButton.setOnClickListener(view -> {
//            showSOSConfirmationDialog();
            sendHelpMessage();
            Toast.makeText(this, "Sos alerted", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            long currentTime = System.currentTimeMillis();

            if (keyCode == lastKeyCode && (currentTime - firstPressTime) < MULTI_PRESS_INTERVAL) {
                pressCount++;
            } else {
                pressCount = 1;
            }

            firstPressTime = currentTime;
            lastKeyCode = keyCode;

            if (pressCount == 3) { // Check if pressed 3 times
                stopSOSAlert();
                pressCount = 0; // Reset count
            }

            return true; // Consume the event
        }
        return super.onKeyDown(keyCode, event);
    }

    // Method to show the SOS confirmation dialog
    private void showSOSConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm SOS")
                .setMessage("You are about to trigger SOS actions. Are you sure?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // User clicked "Yes", trigger SOS actions
                    triggerSOSAlert();
                    Toast.makeText(this, "SOS Alerted!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", (dialog, which) -> {
                    // User clicked "No", dismiss the dialog
                    dialog.dismiss();
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }


    private MediaPlayer mediaPlayer;
    private boolean isSOSActive = false;

//    public void sendSOSMessage() {
//        String phoneNumber = "+919594693842"; // Replace with emergency contact
//        String message = "🚨 SOS Alert! Help needed at my location! 📍http://maps.google.com/?q=latitude,longitude";
//
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
//            SmsManager smsManager = SmsManager.getDefault();
//            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
//            Toast.makeText(this, "SOS message sent!", Toast.LENGTH_SHORT).show();
//        } else {
//            Toast.makeText(this, "SMS permission required!", Toast.LENGTH_SHORT).show();
//        }
//    }

    private void sendHelpMessage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            sendSms();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
        }
    }

    private void sendSms() {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(PHONE_NUMBER, null, HELP_MESSAGE, null, null);
            Toast.makeText(this, "Help message sent!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send SMS.", Toast.LENGTH_SHORT).show();
        }
    }

    private void triggerSOSAlert() {
        SharedPreferences sharedPreferences = getSharedPreferences("SOSSettings", MODE_PRIVATE);

        // Get saved settings
        String contact1 = sharedPreferences.getString("contact1", "");
        String contact2 = sharedPreferences.getString("contact2", "");
        String contact3 = sharedPreferences.getString("contact3", "");
        String messageBody = sharedPreferences.getString("messageBody", "Emergency! I need help!");
        boolean playSound = sharedPreferences.getBoolean("playSound", true);
        boolean sendSMS = sharedPreferences.getBoolean("sendSMS", true);
        boolean sendNotification = sharedPreferences.getBoolean("sendNotification", true);

        // Perform SOS actions based on settings
        if (playSound) {
            // Play sound in a loop
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(this, R.raw.sos_sound);
                mediaPlayer.setLooping(true); // Loop the sound
                mediaPlayer.start();
                isSOSActive = true;
            }
        }
//
//        if (sendSMS) {
//            // Send SMS to contacts
//            SmsManager smsManager = SmsManager.getDefault();
//            if (!contact1.isEmpty()) smsManager.sendTextMessage(contact1, null, messageBody, null, null);
//            if (!contact2.isEmpty()) smsManager.sendTextMessage(contact2, null, messageBody, null, null);
//            if (!contact3.isEmpty()) smsManager.sendTextMessage(contact3, null, messageBody, null, null);
//        }
//
//        if (sendNotification) {
//            // Send notification
//            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
//                NotificationChannel channel = new NotificationChannel(
//                        "sos_channel",
//                        "SOS Alerts",
//                        NotificationManager.IMPORTANCE_HIGH
//                );
//                notificationManager.createNotificationChannel(channel);
//            }
//
//            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "sos_channel")
//                    .setSmallIcon(R.drawable.baseline_sos_24)
//                    .setContentTitle("SOS Alert")
//                    .setContentText(messageBody)
//                    .setPriority(NotificationCompat.PRIORITY_HIGH);
//
//            notificationManager.notify(1, builder.build());
//        }

    }

    private void stopSOSAlert() {
        if (mediaPlayer != null && isSOSActive) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            isSOSActive = false;
            Toast.makeText(this, "SOS Alert Stopped!", Toast.LENGTH_SHORT).show();
        }
    }

    // Helper method to load fragments
    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }
}