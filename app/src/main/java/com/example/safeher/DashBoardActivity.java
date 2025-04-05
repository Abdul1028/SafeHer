package com.example.safeher;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Looper;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;
import android.view.KeyEvent;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.Manifest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DashBoardActivity extends AppCompatActivity {
    private static final String TAG = "DashBoardActivity";
    //Permission Codes
    private static final int SMS_PERMISSION_CODE = 100;
    private static final int LOCATION_PERMISSION_CODE = 101;

    //Undefined Location and SMS body
    private static final String PHONE_NUMBER = "9594693842";
    private static final String HELP_MESSAGE = "Help me! I need assistance.";
    private String currentLocation = "Location not available";

    //Variables to track SOS sound [play or stop]
    private MediaPlayer mediaPlayer;
    private boolean isSOSActive = false;

    //Variables to track Volume down action (to stop SOS)
    private static final int MULTI_PRESS_INTERVAL = 600; // Time interval for triple press detection (600ms)
    private long firstPressTime = 0;
    private int pressCount = 0;
    private int lastKeyCode = 0;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private DatabaseReference geoFireRef;
    private GeoFire geoFire;
    private boolean isLocationUpdatesActive = false;
    private String currentUserId;
    private Location latestLocation; // Add variable to store the latest location

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dash_board);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        geoFireRef = FirebaseDatabase.getInstance().getReference("user_locations");
        geoFire = new GeoFire(geoFireRef);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        } else {
            Log.e(TAG, "User not logged in!");
            // finish(); // Or navigate to login
            // return;
        }

        createLocationCallback();

        //Check and get sms sending perms
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
        }

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

        FloatingActionButton sosButton = findViewById(R.id.sosButton);
        sosButton.setOnClickListener(view -> {
            showSOSConfirmationDialog();
            Toast.makeText(this, "Sos alerted", Toast.LENGTH_SHORT).show();
        });
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if (location != null) { // Store the latest location
                        latestLocation = location;
                        Log.d(TAG, "Latest location updated: " + location.getLatitude() + ", " + location.getLongitude());
                        if (currentUserId != null) { // Continue saving to GeoFire
                            geoFire.setLocation(currentUserId, new GeoLocation(location.getLatitude(), location.getLongitude()),
                                    (key, error) -> {
                                        if (error != null) {
                                            Log.e(TAG, "Error saving location to GeoFire: " + error.getMessage());
                                        } else {
                                            // Log removed for brevity, was: Log.d(TAG, "Location saved on server successfully for user: " + key);
                                        }
                                    });
                        }
                    }
                }
            }
        };
    }

    private void startLocationUpdates() {
        if (currentUserId == null) {
            Log.w(TAG, "Cannot start location updates: User ID is null.");
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permissions not granted.");
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5 * 60 * 1000)
                .setMinUpdateIntervalMillis(60 * 1000)
                .setMinUpdateDistanceMeters(200)
                .build();

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            isLocationUpdatesActive = true;
            Log.i(TAG, "Location updates started.");
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException in requestLocationUpdates: " + e.getMessage());
        }
    }

    private void stopLocationUpdates() {
        if (isLocationUpdatesActive) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            isLocationUpdatesActive = false;
            Log.i(TAG, "Location updates stopped.");
        }
    }

    private void checkLocationPermissionAndStartUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Location permission already granted. Starting updates.");
            startLocationUpdates();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(this)
                    .setTitle("Location Permission Needed")
                    .setMessage("This app needs the Location permission to share your location during an SOS and update your position for nearby alerts.")
                    .setPositiveButton("OK", (dialog, which) -> requestLocationPermission())
                    .create()
                    .show();
        } else {
            requestLocationPermission();
        }
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                LOCATION_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Location permission granted by user.");
                startLocationUpdates();
            } else {
                Log.w(TAG, "Location permission denied by user.");
                Toast.makeText(this, "Location permission is required for SOS features.", Toast.LENGTH_LONG).show();
            }
        }
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "SMS permission granted by user.");
                Toast.makeText(this, "SMS permission granted. You may need to trigger SOS again.", Toast.LENGTH_LONG).show();
            } else {
                Log.w(TAG, "SMS permission denied by user.");
                Toast.makeText(this, "SMS permission is required to send SOS messages.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkLocationPermissionAndStartUpdates();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopLocationUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
        if (currentUserId != null) {
            geoFire.removeLocation(currentUserId, (key, error) -> {
                if (error != null) {
                    Log.e(TAG, "Error removing location on destroy: " + error.getMessage());
                } else {
                    Log.d(TAG, "Location removed successfully on destroy for user: " + key);
                }
            });
        }
    }

    // Method to show the SOS confirmation dialog (Float button [SOS] click)
    private void showSOSConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm SOS")
                .setMessage("You are about to trigger SOS actions. Are you sure?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    triggerSOSAlert();
                })
                .setNegativeButton("No", (dialog, which) -> {
                    dialog.dismiss();
                    Toast.makeText(this, "SOS cancelled", Toast.LENGTH_SHORT).show();
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    // Updated SOS Trigger Logic
    private void triggerSOSAlert() {
        if (currentUserId == null) {
            Log.e(TAG, "Cannot trigger SOS: User not logged in.");
            Toast.makeText(this, "Error: Not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (latestLocation == null) {
            Log.w(TAG, "Cannot trigger SOS: Location not available yet.");
            Toast.makeText(this, "Location not available yet. Please wait.", Toast.LENGTH_LONG).show();
            // Optionally, try fetching last known location as a fallback here?
            // fusedLocationClient.getLastLocation()... (but adds complexity)
            return;
        }

        Log.i(TAG, "SOS Triggered by user: " + currentUserId);

        // --- Step 1: Write SOS alert to Firebase (for Cloud Function) ---
        DatabaseReference sosRef = FirebaseDatabase.getInstance().getReference("sos_alerts");
        String alertId = sosRef.push().getKey(); // Generate unique ID

        if (alertId != null) {
            java.util.Map<String, Object> sosData = new java.util.HashMap<>();
            sosData.put("user_id", currentUserId);
            sosData.put("lat", latestLocation.getLatitude());
            sosData.put("lng", latestLocation.getLongitude());
            sosData.put("timestamp", System.currentTimeMillis());

            sosRef.child(alertId).setValue(sosData)
                    .addOnSuccessListener(aVoid -> Log.i(TAG, "SOS alert written to Firebase: " + alertId))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to write SOS alert to Firebase", e));
        } else {
            Log.e(TAG, "Failed to generate alert ID for Firebase.");
        }

        // --- Step 2: Perform In-App Actions based on SharedPreferences ---
        SharedPreferences sharedPreferences = getSharedPreferences("SOSSettings", MODE_PRIVATE);

        boolean playSound = sharedPreferences.getBoolean("playSound", true);
        boolean sendSMSPref = sharedPreferences.getBoolean("sendSMS", true);

        // Play Sound Action
        if (playSound) {
            Log.d(TAG, "Playing SOS sound based on preference.");
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(this, R.raw.sos_sound);
                if (mediaPlayer != null) {
                    mediaPlayer.setLooping(true);
                    mediaPlayer.start();
                    isSOSActive = true;
                } else {
                    Log.e(TAG, "Failed to create MediaPlayer for SOS sound.");
                    Toast.makeText(this, "Error playing sound.", Toast.LENGTH_SHORT).show();
                }
            } else if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start(); // Resume if already created but stopped
                isSOSActive = true;
            }
        }

        // Send SMS Action
        if (sendSMSPref) {
            Log.d(TAG, "Attempting to send SMS based on preference.");
            // Get contacts and message from preferences
            String contact1 = sharedPreferences.getString("contact1", "");
            String contact2 = sharedPreferences.getString("contact2", "");
            String contact3 = sharedPreferences.getString("contact3", "");
            String messageBody = sharedPreferences.getString("messageBody", "Emergency! I need help!");

            java.util.ArrayList<String> contacts = new java.util.ArrayList<>();
            if (!contact1.isEmpty()) contacts.add(contact1);
            if (!contact2.isEmpty()) contacts.add(contact2);
            if (!contact3.isEmpty()) contacts.add(contact3);

            if (!contacts.isEmpty()) {
                // Construct message with location
                String locationUrl = "https://maps.google.com/?q=" + latestLocation.getLatitude() + "," + latestLocation.getLongitude();
                String finalMessage = messageBody + "\nMy current location: " + locationUrl;

                // Check permission and send
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                    sendSmsToContacts(contacts, finalMessage);
                } else {
                    Log.w(TAG, "SMS permission not granted. Requesting permission.");
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
                    // Store necessary info to send SMS later if permission granted in onRequestPermissionsResult?
                    // This part needs careful handling for a robust solution.
                    Toast.makeText(this, "SMS Permission needed to send alerts.", Toast.LENGTH_LONG).show();
                }
            } else {
                Log.w(TAG, "Send SMS preference is true, but no contacts are saved in settings.");
                Toast.makeText(this, "No SOS contacts saved in settings.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Modified SMS Sending Logic
    private void sendSmsToContacts(java.util.ArrayList<String> contacts, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            int sentCount = 0;
            for (String contact : contacts) {
                if (contact != null && !contact.trim().isEmpty()) {
                    try {
                        smsManager.sendTextMessage(contact.trim(), null, message, null, null);
                        sentCount++;
                        Log.i(TAG, "SMS sent successfully to: " + contact.trim());
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to send SMS to: " + contact.trim(), e);
                        Toast.makeText(this, "Failed to send SMS to " + contact.trim(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
            if (sentCount > 0) {
                Toast.makeText(this, "Help message sent to " + sentCount + " contact(s)!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "General error getting SmsManager or sending SMS.", e);
            Toast.makeText(this, "Failed to send SMS.", Toast.LENGTH_SHORT).show();
        }
    }

    //Listen for triple press event on volume button
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

    //Call this method to stop SOSa6ui0\,m,
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