
# 🛡️ Women Safety App – SOS Alert System (Cloud Functions, Scalable, Real-Time)

## 🎯 Objective  
Build a scalable SOS alert system using Firebase, with real-time alerts to guardians and nearby users, leveraging **Firebase Cloud Functions**, **GeoFire**, and **FCM**.

## ⚙️ Tech Stack  

| Component                     | Tool / Library                         |
|------------------------------|----------------------------------------|
| Auth                         | Firebase Authentication                |
| Realtime Data & Geolocation  | Firebase Realtime DB + GeoFire         |
| Push Notifications           | Firebase Cloud Messaging (FCM)         |
| Location Tracking            | FusedLocationProviderClient (Android) |
| SMS / Twilio / Alerts        | Firebase Cloud Functions (Node.js)     |

## ✅ Updated System Architecture (With Cloud Functions)

```mermaid
graph TD;
    A[App Launch] --> B[FCM Token Generated & Stored]
    B --> C[Location Tracking >200m → GeoFire]
    C --> D[User Triggers SOS]
    D --> E[Write SOS to Firebase]
    E --> F[Cloud Function Triggered]
    F --> G[1. Notify Guardians (SMS/Push)]
    F --> H[2. Query Nearby Users via GeoFire]
    H --> I[Get FCM Tokens of Nearby Users]
    I --> J[Send Push Notifications via FCM]
```

## 📂 Firebase Database Structure

```json
{
  "users": {
    "userId123": {
      "name": "Zain",
      "phone": "1234567890",
      "fcm_token": "abc123xyz"
    }
  },
  "user_locations": {
    "userId123": {
      "l": [28.6, 77.2]
    }
  },
  "sos_alerts": {
    "alertId123": {
      "user_id": "userId123",
      "lat": 28.6,
      "lng": 77.2,
      "timestamp": 1700000000000
    }
  }
}
```

## 🧩 Core Modules (Cursor-friendly format)

### 1. Save FCM Token on App Launch

```java
FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
    if (!task.isSuccessful()) return;
    String token = task.getResult();
    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(userId);
    ref.child("fcm_token").setValue(token);
});
```

Optional: Handle token refresh

```java
FirebaseMessaging.getInstance().addOnNewTokenListener(token -> {
    saveFCMTokenToFirebase(token);
});
```

### 2. Efficient Location Tracking with GeoFire

```java
LocationRequest request = LocationRequest.create()
    .setInterval(5 * 60 * 1000)
    .setFastestInterval(60 * 1000)
    .setSmallestDisplacement(200)
    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper());

LocationCallback callback = new LocationCallback() {
    public void onLocationResult(LocationResult result) {
        Location loc = result.getLastLocation();
        GeoFire geoFire = new GeoFire(FirebaseDatabase.getInstance().getReference("user_locations"));
        geoFire.setLocation(userId, new GeoLocation(loc.getLatitude(), loc.getLongitude()));
    }
};
```

### 3. Trigger SOS (App Code)

```java
Map<String, Object> sosData = new HashMap<>();
sosData.put("user_id", userId);
sosData.put("lat", currentLat);
sosData.put("lng", currentLng);
sosData.put("timestamp", System.currentTimeMillis());

FirebaseDatabase.getInstance().getReference("sos_alerts").push().setValue(sosData);
```

→ This triggers the **Firebase Cloud Function**.

## ⚙️ 4. Cloud Function – SOS Handler

```javascript
exports.onSosCreated = functions.database
  .ref('/sos_alerts/{alertId}')
  .onCreate(async (snapshot, context) => {
    const sos = snapshot.val();
    const { lat, lng, user_id } = sos;

    await notifyGuardians(user_id, lat, lng);

    const geoFireRef = db.ref('user_locations');
    const geoFire = new GeoFire(geoFireRef);
    const geoQuery = geoFire.query({ center: [lat, lng], radius: 1 });

    geoQuery.on('key_entered', async (nearbyUserId, location) => {
      if (nearbyUserId !== user_id) {
        const tokenSnap = await db.ref(`users/${nearbyUserId}/fcm_token`).get();
        if (tokenSnap.exists()) {
          await sendPush(tokenSnap.val(), lat, lng);
        }
      }
    });
  });
```

### 5. Send Push Notification via FCM (in Cloud Function)

```javascript
async function sendPush(token, lat, lng) {
  const message = {
    token,
    data: {
      title: "🚨 SOS Alert Nearby!",
      body: "Someone needs help near you.",
      lat: lat.toString(),
      lng: lng.toString()
    }
  };
  await admin.messaging().send(message);
}
```

### 6. Handle Push in FirebaseMessagingService (Android)

```java
@Override
public void onMessageReceived(RemoteMessage msg) {
    double lat = Double.parseDouble(msg.getData().get("lat"));
    double lng = Double.parseDouble(msg.getData().get("lng"));

    Intent intent = new Intent(this, SosMapActivity.class);
    intent.putExtra("lat", lat);
    intent.putExtra("lng", lng);

    PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "sos_channel")
        .setSmallIcon(R.drawable.ic_sos)
        .setContentTitle("🚨 SOS Nearby")
        .setContentText("Tap to help someone near you.")
        .setContentIntent(pi)
        .setAutoCancel(true);

    NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    manager.notify(1, builder.build());
}
```

## ⚡️ Performance & Firebase Optimization

| Feature               | Optimization                                |
|----------------------|---------------------------------------------|
| Location Tracking     | Displacement >200m only                     |
| Cloud Functions       | Handle GeoQuery + Notification server-side |
| Geo Queries (Nearby) | Real-time via GeoFire                       |
| Token Usage           | Only used when SOS is triggered             |
| Notification Payload | Lightweight, map intent on click            |

## ✅ Final Developer Checklist

- [x] Store FCM token on launch/login
- [x] Update location only if moved >200m
- [x] Save location using GeoFire
- [x] Trigger SOS to create entry in `sos_alerts`
- [x] Use **Cloud Function** to:
   - Notify guardians
   - Find nearby users
   - Fetch FCM tokens
   - Send FCM alerts
- [x] Open map activity on notification tap
