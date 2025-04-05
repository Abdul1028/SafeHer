package com.example.safeher;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

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
} 