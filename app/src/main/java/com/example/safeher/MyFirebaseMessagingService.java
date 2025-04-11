package com.example.safeher;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    public static final String SOS_CHANNEL_ID = "sos_channel";

    /**
     * Called if the FCM registration token is updated. This may occur if the previous token had expired,
     * the app deletes Instance ID data, or the app is restored on a new device.
     * You should send the new token to your application server.
     *
     * @param token The new token.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(token);
    }

    /**
     * Persist token to third-party servers.
     * <p>
     * Modify this method to associate the user's FCM registration token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            String userId = firebaseUser.getUid();
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(userId);
            Log.d(TAG, "Saving refreshed FCM token for user: " + userId);
            ref.child("fcm_token").setValue(token)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Refreshed FCM token saved successfully for user: " + userId))
                    .addOnFailureListener(e -> Log.w(TAG, "Error saving refreshed FCM token for user: " + userId, e));
        } else {
            Log.w(TAG, "Cannot save refreshed FCM token: No user logged in.");
        }
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "---- FCM Message Received ----");
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        Map<String, String> data = remoteMessage.getData();
        Log.d(TAG, "Message Data payload: " + data);

        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body (if present): " + remoteMessage.getNotification().getBody());
        }

        // Simplified Check: Assuming any message with lat/lng is an SOS alert for now
        // Remove the 'type' check temporarily for debugging if Cloud Function isn't sending it
        if (data != null && data.containsKey("lat") && data.containsKey("lng")) {
             Log.i(TAG, "Potential SOS Alert message detected based on lat/lng presence.");
             // Ensure this runs on the main thread if showing Toasts
             new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                 Toast.makeText(getApplicationContext(), "SOS Message Received", Toast.LENGTH_SHORT).show();
             });
             handleSosAlert(data);
        } else {
            Log.d(TAG, "Received FCM message without lat/lng data, ignoring for SOS.");
        }
    }

    /**
     * Handle the incoming SOS alert data and display a notification.
     * @param data The data payload from the FCM message.
     */
    private void handleSosAlert(Map<String, String> data) {
        Log.d(TAG, "---- handleSosAlert START ----");
        String title = data.getOrDefault("title", "SOS Alert Nearby!");
        String body = data.getOrDefault("body", "Someone needs help near you. Tap for details.");
        String latStr = data.get("lat");
        String lngStr = data.get("lng");
        Log.d(TAG, "Data received: title="+title+", body="+body+", latStr="+latStr+", lngStr="+lngStr);

        if (latStr == null || lngStr == null) {
            Log.e(TAG, "SOS Alert data missing lat/lng. Cannot proceed.");
            return;
        }

        double lat = 0.0, lng = 0.0;
        try {
            lat = Double.parseDouble(latStr);
            lng = Double.parseDouble(lngStr);
            Log.d(TAG, "Parsed coordinates: lat=" + lat + ", lng=" + lng);

            // Intent creation
            Log.d(TAG, "Creating Intent for SosMapActivity.");
            Intent intent = new Intent(this, SosMapActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("sos_alert_lat", lat);
            intent.putExtra("sos_alert_lng", lng);
            Log.d(TAG, "Intent created with extras: lat=" + intent.getDoubleExtra("sos_alert_lat", -1) + ", lng=" + intent.getDoubleExtra("sos_alert_lng", -1));

            // PendingIntent creation
            Log.d(TAG, "Creating PendingIntent.");
            int pendingIntentFlags = PendingIntent.FLAG_ONE_SHOT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                pendingIntentFlags |= PendingIntent.FLAG_IMMUTABLE;
            } else {
                pendingIntentFlags |= PendingIntent.FLAG_UPDATE_CURRENT;
            }
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                    pendingIntentFlags);
            Log.d(TAG, "PendingIntent created: " + (pendingIntent != null));

            // Notification building
            Log.d(TAG, "Building notification.");
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, SOS_CHANNEL_ID)
                    .setSmallIcon(R.drawable.baseline_sos_24) // Ensure you have this drawable
                    .setContentTitle(title)
                    .setContentText(body)
                    .setAutoCancel(true)
                    .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent);
            Log.d(TAG, "Notification builder prepared.");

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            // Channel Creation (O+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                 Log.d(TAG, "Ensuring notification channel exists (Oreo+).");
                 NotificationChannel channel = new NotificationChannel(SOS_CHANNEL_ID,
                        "SOS Alerts",
                        NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("Notifications for nearby SOS alerts");
                notificationManager.createNotificationChannel(channel);
            }

            // Show Notification
            int notificationId = (int) System.currentTimeMillis();
            Log.i(TAG, "Displaying notification with ID: " + notificationId);
            notificationManager.notify(notificationId, notificationBuilder.build());
            Log.d(TAG, "---- handleSosAlert END ----");

        } catch (NumberFormatException e) {
            Log.e(TAG, "Failed to parse lat/lng from SOS alert data", e);
            // Ensure this runs on the main thread if showing Toasts
             new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                 Toast.makeText(getApplicationContext(), "Error: Invalid location data in SOS.", Toast.LENGTH_LONG).show();
             });
        } catch (Exception e) {
            Log.e(TAG, "Error displaying SOS notification", e);
             new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                 Toast.makeText(getApplicationContext(), "Error showing SOS notification.", Toast.LENGTH_LONG).show();
             });
        }
    }
} 