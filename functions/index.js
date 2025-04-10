/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");
const GeoFire = require("geofire").GeoFire; // Use .GeoFire for constructor
const { onValueCreated } = require("firebase-functions/v2/database"); // Import v2 trigger

// Create and deploy your first functions
https://firebase.google.com/docs/functions/get-started

// exports.helloWorld = onRequest((request, response) => {
//   logger.info("Hello logs!", {structuredData: true});
//   response.send("Hello from Firebase!");
// });

// Initialize Firebase Admin SDK (only once)
admin.initializeApp();

const db = admin.database();
const messaging = admin.messaging();

// Define the radius for nearby search (in kilometers)
const NEARBY_RADIUS_KM = 1;

/**
 * Triggered when a new SOS alert is created in /sos_alerts.
 * Finds nearby users using GeoFire and sends them an FCM notification.
 */
exports.notifyNearbyUsersOnSos = onValueCreated(
  "/sos_alerts/{alertId}",
  async (event) => {
    // ---- VERY FIRST LOG ----
    console.log(`Function notifyNearbyUsersOnSos triggered for event ID: ${event.id}`);
    // ------------------------

    // Log the entire event object to understand its structure
    console.log("Received event:", JSON.stringify(event, null, 2));

    // Get the data that was created.
    // Based on logs, data is directly in event.data, not event.data.after
    const sosData = event.data;
    const alertId = event.params.alertId;

    // Check if sosData exists (it should based on logs, but good practice)
    if (!sosData) {
        console.error(`SOS data (event.data) is missing or null for alertId: ${alertId}. Event:`, JSON.stringify(event, null, 2));
        return null; // Exit if data is missing
    }

    // Log the details of the created alert
    console.log(`New SOS Alert Created - ID: ${alertId}, Data:`, JSON.stringify(sosData.val(), null, 2));

    // Log sosData raw structure before destructuring
    console.log('sosData (DataSnapshot) before val():', sosData);

    // Get the plain JavaScript object from the snapshot
    const plainData = sosData.val();
    console.log('Plain data object after val():', plainData);

    // Check if plainData exists after calling val()
    if (!plainData) {
        console.error(`Data retrieved from snapshot was null or undefined for alertId: ${alertId}.`);
        return null;
    }

    // Destructure from the plain data object
    const { lat, lng, user_id: sosUserId } = plainData;

    // Log values immediately after destructuring
    console.log(`Values after destructuring - lat: ${lat}, lng: ${lng}, sosUserId: ${sosUserId}`);

    if (lat == null || lng == null || !sosUserId) {
      console.error(`Invalid SOS data after destructuring - lat: ${lat}, lng: ${lng}, sosUserId: ${sosUserId}`, plainData);
      return null;
    }

    console.log(`Processing SOS alert: ${alertId} from user: ${sosUserId} at [${lat}, ${lng}]`);

    const geoFireRef = db.ref('user_locations');
    const geoFire = new GeoFire(geoFireRef);

    // Create a GeoQuery centered around the SOS location
    const geoQuery = geoFire.query({
      center: [lat, lng],
      radius: NEARBY_RADIUS_KM,
    });

    const nearbyUserPromises = [];

    // Listener for when a key (userId) enters the query radius
    const onKeyEnteredRegistration = geoQuery.on('key_entered', (nearbyUserId, location, distance) => {
      console.log(`User ${nearbyUserId} entered radius at [${location}], distance: ${distance.toFixed(2)} km.`);

      // Don't notify the user who triggered the SOS
      if (nearbyUserId === sosUserId) {
        console.log(`Skipping notification for triggering user: ${nearbyUserId}`);
        return;
      }

      // Create a promise to fetch the token and send the notification
      const promise = db.ref(`/users/${nearbyUserId}/fcm_token`).once('value')
        .then(tokenSnapshot => {
          if (!tokenSnapshot.exists() || !tokenSnapshot.val()) {
            console.log(`No FCM token found for nearby user: ${nearbyUserId}`);
            return null; // No token, can't notify
          }
          const fcmToken = tokenSnapshot.val();
          console.log(`Found FCM token for nearby user: ${nearbyUserId}`);
          return sendPushNotification(fcmToken, lat, lng, distance);
        })
        .catch(error => {
          console.error(`Error fetching token or sending push for user ${nearbyUserId}:`, error);
          return null; // Indicate failure for this user
        });

      nearbyUserPromises.push(promise);
    });
    
    // Listener for when the GeoQuery is ready (all initial keys loaded)
    // We need this to know when to stop listening and await results
    return new Promise((resolve, reject) => {
      geoQuery.on('ready', () => {
        console.log('GeoQuery is ready. All initial keys processed.');
        // Cancel the query to stop listening for further events
        geoQuery.cancel();
        // Cancel the key_entered listener registration
        onKeyEnteredRegistration.cancel();

        // Wait for all the token fetching and notification sending promises
        Promise.all(nearbyUserPromises)
          .then(results => {
            const successCount = results.filter(r => r !== null).length;
            console.log(`Finished processing nearby users for alert ${alertId}. Sent ${successCount} notifications.`);
            resolve(); // Resolve the main promise when all sub-tasks are done
          })
          .catch(error => {
              console.error(`Error processing nearby user promises for alert ${alertId}:`, error);
              reject(error); // Reject the main promise on error
          });
      });

      // Add error handling for the GeoQuery itself
        geoQuery.on('error', (error) => {
            console.error(`GeoQuery error for alert ${alertId}:`, error);
            geoQuery.cancel(); // Stop the query on error
            onKeyEnteredRegistration.cancel();
            reject(new Error(`GeoQuery failed: ${error}`)); // Reject the main promise
        });
    });

    // --- End of Re-enabled Logic ---
  });

/**
 * Sends an FCM push notification to a specific device token.
 * @param {string} token The FCM registration token of the target device.
 * @param {number} lat The latitude of the SOS alert.
 * @param {number} lng The longitude of the SOS alert.
 * @param {number} distance The distance of the recipient from the alert (km).
 * @returns {Promise<string>} A promise that resolves with the message ID on success.
 */
async function sendPushNotification(token, lat, lng, distance) {
    const distanceText = distance < 1 ? `${(distance * 1000).toFixed(0)}m` : `${distance.toFixed(1)}km`;
    const message = {
        token: token,
        // Using 'notification' payload for simple display when app is in background
        notification: {
            title: `🚨 SOS Alert Nearby! (${distanceText} away)`,
            body: `Someone needs help near your location. Tap for details.`,
            // icon: 'stock_ticker_update', // Optional: specify an icon
            // color: '#ff0000' // Optional: specify color
        },
        // Using 'data' payload to send info for the app to handle when active
        // or when notification is tapped
        data: {
            title: `🚨 SOS Alert Nearby! (${distanceText} away)`, // Can repeat title/body here
            body: `Someone needs help near your location. Tap for details.`,
            lat: lat.toString(),
            lng: lng.toString(),
            type: 'SOS_ALERT' // Add a type for easier handling in the app
        },
        // Android specific options (optional)
        android: {
            priority: 'high', // Ensure high priority delivery
            notification: {
                sound: 'default', // Use default notification sound
                // channel_id: "sos_channel" // Important: Match channel ID created in app for Android 8+
            }
        },
        // APNS specific options for iOS (optional)
        // apns: { ... }
    };

    console.log(`Sending FCM message to token: ${token.substring(0, 20)}...`);
    try {
        const response = await messaging.send(message);
        console.log('Successfully sent message:', response);
        return response; // Indicate success
    } catch (error) {
        console.error('Error sending FCM message:', error);
        // Decide how to handle specific errors (e.g., invalid token)
        if (error.code === 'messaging/registration-token-not-registered') {
            // Optionally: Clean up the invalid token from the database
            console.log(`Token ${token.substring(0,20)}... is invalid. Consider removing it.`);
        }
        throw error; // Re-throw to be caught by the caller promise chain
    }
}
