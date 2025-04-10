package com.example.safeher;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

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

        // Log entire message details for debugging
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        Map<String, String> data = remoteMessage.getData();
        Log.d(TAG, "Message Data payload: " + data);

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }

        // Check if it's our SOS alert type from the data payload
        if (data != null && "SOS_ALERT".equals(data.get("type"))) {
            Log.i(TAG, "Received SOS Alert message.");
            handleSosAlert(data);
        } else {
            Log.d(TAG, "Received other type of FCM message, ignoring for SOS.");
            // Handle other types of messages if needed
        }
    }

    /**
     * Handle the incoming SOS alert data and display a notification.
     * @param data The data payload from the FCM message.
     */
    private void handleSosAlert(Map<String, String> data) {
        String title = data.getOrDefault("title", "SOS Alert Nearby!");
        String body = data.getOrDefault("body", "Someone needs help near you. Tap for details.");
        String latStr = data.get("lat");
        String lngStr = data.get("lng");

        if (latStr == null || lngStr == null) {
            Log.e(TAG, "SOS Alert data missing lat/lng.");
            return;
        }

        try {
            double lat = Double.parseDouble(latStr);
            double lng = Double.parseDouble(lngStr);

            // Intent to launch when notification is tapped
            // TODO: Create SosMapActivity.class
            Intent intent = new Intent(this, SosMapActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("sos_alert_lat", lat);
            intent.putExtra("sos_alert_lng", lng);
            // Potentially add more flags or data if needed

            // Use FLAG_IMMUTABLE or FLAG_MUTABLE based on Android version requirements
            int pendingIntentFlags = PendingIntent.FLAG_ONE_SHOT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                pendingIntentFlags |= PendingIntent.FLAG_IMMUTABLE; // Use IMMUTABLE for S+
            } else {
                pendingIntentFlags |= PendingIntent.FLAG_UPDATE_CURRENT; // Or keep UPDATE_CURRENT if needed pre-S
            }

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                    pendingIntentFlags);

            // Build the notification
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, SOS_CHANNEL_ID)
                    .setSmallIcon(R.drawable.baseline_sos_24) // Ensure you have this drawable
                    .setContentTitle(title)
                    .setContentText(body)
                    .setAutoCancel(true) // Dismiss notification when tapped
                    .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI) // Default sound
                    .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority for alerts
                    .setContentIntent(pendingIntent); // Set the intent to fire when tapped

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            // Create the Notification Channel for Android 8.0 (Oreo) and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(SOS_CHANNEL_ID,
                        "SOS Alerts", // User-visible name of the channel
                        NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("Notifications for nearby SOS alerts"); // User-visible description
                // Configure channel properties (optional)
                // channel.enableLights(true);
                // channel.setLightColor(Color.RED);
                // channel.enableVibration(true);
                notificationManager.createNotificationChannel(channel);
            }

            // Show the notification (use a unique ID, e.g., based on timestamp or a fixed one)
            int notificationId = (int) System.currentTimeMillis();
            notificationManager.notify(notificationId, notificationBuilder.build());
            Log.i(TAG, "SOS Alert notification displayed. ID: " + notificationId);

        } catch (NumberFormatException e) {
            Log.e(TAG, "Failed to parse lat/lng from SOS alert data", e);
        } catch (Exception e) {
            Log.e(TAG, "Error displaying SOS notification", e);
        }
    }
} 